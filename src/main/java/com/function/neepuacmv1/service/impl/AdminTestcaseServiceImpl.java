package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.function.neepuacmv1.constant.ProblemFileConstants;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminTestcaseSaveReq;
import com.function.neepuacmv1.dto.resp.AdminTestcaseListResp;
import com.function.neepuacmv1.dto.resp.AdminUploadResp;
import com.function.neepuacmv1.entity.Problem;
import com.function.neepuacmv1.entity.ProblemTestcase;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.mapper.AdminProblemQueryMapper;
import com.function.neepuacmv1.mapper.ProblemMapper;
import com.function.neepuacmv1.mapper.ProblemTestcaseMapper;
import com.function.neepuacmv1.service.AdminTestcaseService;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class AdminTestcaseServiceImpl implements AdminTestcaseService {

    private final ProblemTestcaseMapper testcaseMapper;
    private final ProblemMapper problemMapper;
    private final AdminProblemQueryMapper queryMapper;
    private final RedisUtil redisUtil;

    @Value("${acm.problem.data-root:" + ProblemFileConstants.DEFAULT_ROOT + "}")
    private String dataRoot;

    public AdminTestcaseServiceImpl(ProblemTestcaseMapper testcaseMapper,
                                    ProblemMapper problemMapper,
                                    AdminProblemQueryMapper queryMapper,
                                    RedisUtil redisUtil) {
        this.testcaseMapper = testcaseMapper;
        this.problemMapper = problemMapper;
        this.queryMapper = queryMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result list(Long problemId) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, problemId).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            List<Map<String,Object>> rows = queryMapper.listTestcases(problemId);
            List<AdminTestcaseListResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminTestcaseListResp(
                        toLong(r.get("id")),
                        toLong(r.get("problem_id")),
                        toInt(r.get("is_sample")),
                        toInt(r.get("is_public")),
                        toInt(r.get("score")),
                        toInt(r.get("order_index")),
                        str(r.get("input_path")),
                        str(r.get("output_path")),
                        (java.time.LocalDateTime) r.get("updated_at")
                ));
            }
            return Result.ok(list);
        } catch (Exception e) {
            return Result.fail("获取测试点列表失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result upload(Long problemId, MultipartFile[] files) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            if (files == null || files.length == 0) return Result.fail("文件不能为空");

            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, problemId).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            List<String> fail = new ArrayList<>();
            int ok = 0;

            // 支持：直接上传多个 .in/.out 文件 或 上传 zip
            Map<String, byte[]> pool = new HashMap<>();

            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                String name = f.getOriginalFilename();
                if (ValidationUtil.isBlank(name)) {
                    fail.add("存在文件名为空的文件");
                    continue;
                }
                name = name.trim();

                if (name.toLowerCase().endsWith(".zip")) {
                    // unzip
                    unzipToPool(f.getInputStream(), pool);
                } else {
                    pool.put(name, f.getBytes());
                }
            }

            // 配对：xxx.in 与 xxx.out
            Set<String> bases = new HashSet<>();
            for (String fn : pool.keySet()) {
                if (fn.endsWith(ProblemFileConstants.IN_SUFFIX)) bases.add(fn.substring(0, fn.length() - 3));
                if (fn.endsWith(ProblemFileConstants.OUT_SUFFIX)) bases.add(fn.substring(0, fn.length() - 4));
            }

            // 当前最大 order_index + 1
            Integer maxOrder = testcaseMapper.selectList(new LambdaQueryWrapper<ProblemTestcase>()
                            .eq(ProblemTestcase::getProblemId, problemId))
                    .stream().map(ProblemTestcase::getOrderIndex).filter(Objects::nonNull)
                    .max(Integer::compareTo).orElse(0);

            int order = maxOrder + 1;

            for (String base : bases) {
                String inName = base + ProblemFileConstants.IN_SUFFIX;
                String outName = base + ProblemFileConstants.OUT_SUFFIX;
                byte[] inBytes = pool.get(inName);
                byte[] outBytes = pool.get(outName);

                if (inBytes == null || outBytes == null) {
                    fail.add("缺少配对文件：" + base);
                    continue;
                }

                // 写入本地文件
                Path dir = Paths.get(dataRoot, String.valueOf(problemId), "testcases", UUID.randomUUID().toString());
                Files.createDirectories(dir);

                Path inPath = dir.resolve("data.in");
                Path outPath = dir.resolve("data.out");
                Files.write(inPath, inBytes);
                Files.write(outPath, outBytes);

                ProblemTestcase tc = new ProblemTestcase();
                tc.setProblemId(problemId);
                tc.setInputPath(inPath.toString());
                tc.setOutputPath(outPath.toString());
                tc.setScore(0);
                tc.setIsSample(0);
                tc.setIsPublic(0);
                tc.setOrderIndex(order++);
                tc.setMd5In(md5(inBytes));
                tc.setMd5Out(md5(outBytes));

                int rows = testcaseMapper.insert(tc);
                if (rows > 0) ok++;
                else fail.add("写入数据库失败：" + base);
            }

            // 题目详情缓存失效（前台可能显示样例/统计）
            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + problemId);

            return Result.ok(new AdminUploadResp(ok, fail.size(), fail));
        } catch (Exception e) {
            return Result.fail("上传测试点失败：" + e.getMessage());
        }
    }

    @Override
    public Result setSample(Long testcaseId, Integer isSample) {
        try {
            if (testcaseId == null) return Result.fail("testcaseId不能为空");
            if (isSample == null || (isSample != 0 && isSample != 1)) return Result.fail("isSample只能为0或1");

            ProblemTestcase tc = testcaseMapper.selectById(testcaseId);
            if (tc == null) return Result.fail("测试点不存在");

            ProblemTestcase upd = new ProblemTestcase();
            upd.setId(testcaseId);
            upd.setIsSample(isSample);

            // 样例通常公开：手册说样例构造/移除；这里默认把样例设为 public=1
            if (isSample == 1) upd.setIsPublic(1);

            int rows = testcaseMapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + tc.getProblemId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("设置样例失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result deleteBatch(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");

            List<ProblemTestcase> list = testcaseMapper.selectList(new LambdaQueryWrapper<ProblemTestcase>()
                    .in(ProblemTestcase::getId, req.getIds()));
            if (list == null || list.isEmpty()) return Result.ok();

            // 删除文件 + 删除 DB
            Set<Long> problemIds = new HashSet<>();
            for (ProblemTestcase tc : list) {
                problemIds.add(tc.getProblemId());
                safeDelete(tc.getInputPath());
                safeDelete(tc.getOutputPath());
                // 删除目录
                safeDeleteParent(tc.getInputPath());
                safeDeleteParent(tc.getOutputPath());
            }
            int rows = testcaseMapper.deleteBatchIds(req.getIds());
            if (rows <= 0) return Result.fail("删除失败");

            for (Long pid : problemIds) redisUtil.delete(RedisKeys.PROBLEM_DETAIL + pid);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除测试点失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result save(AdminTestcaseSaveReq req) {
        try {
            if (req == null || req.getProblemId() == null) return Result.fail("problemId不能为空");
            if (req.getItems() == null) return Result.fail("items不能为空");

            // 校验题目存在
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, req.getProblemId()).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            for (AdminTestcaseSaveReq.Item it : req.getItems()) {
                if (it.getId() == null) continue;
                ProblemTestcase tc = testcaseMapper.selectById(it.getId());
                if (tc == null || !req.getProblemId().equals(tc.getProblemId())) {
                    return Result.fail("存在不属于该题目的测试点：" + it.getId());
                }

                ProblemTestcase upd = new ProblemTestcase();
                upd.setId(it.getId());
                if (it.getIsSample() != null) upd.setIsSample(it.getIsSample());
                if (it.getIsPublic() != null) upd.setIsPublic(it.getIsPublic());
                if (it.getScore() != null) upd.setScore(it.getScore());
                if (it.getOrderIndex() != null) upd.setOrderIndex(it.getOrderIndex());

                testcaseMapper.updateById(upd);
            }

            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + req.getProblemId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("保存测试点失败：" + e.getMessage());
        }
    }

    @Override
    public Result buildDownloadZip(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");

            List<ProblemTestcase> list = testcaseMapper.selectList(new LambdaQueryWrapper<ProblemTestcase>()
                    .in(ProblemTestcase::getId, req.getIds()));
            if (list == null || list.isEmpty()) return Result.fail("未找到测试点");

            // 输出到临时目录
            Path tmpDir = Files.createTempDirectory("acm-tc-zip-");
            Path zipPath = tmpDir.resolve("testcases.zip");

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                int idx = 1;
                for (ProblemTestcase tc : list) {
                    // 统一命名：{idx}.in / {idx}.out
                    addFile(zos, idx + ".in", tc.getInputPath());
                    addFile(zos, idx + ".out", tc.getOutputPath());
                    idx++;
                }
            }

            Map<String,Object> resp = new HashMap<>();
            resp.put("zipPath", zipPath.toString()); // Controller 负责读这个文件并输出
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("构建下载包失败：" + e.getMessage());
        }
    }

    // ---------- helpers ----------
    private static void unzipToPool(InputStream in, Map<String, byte[]> pool) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (name == null) continue;
                // 防目录穿越：只取文件名
                name = Paths.get(name).getFileName().toString();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                zis.transferTo(bos);
                pool.put(name, bos.toByteArray());
            }
        }
    }

    private static String md5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void addFile(ZipOutputStream zos, String entryName, String path) throws IOException {
        if (path == null) return;
        File f = new File(path);
        if (!f.exists() || !f.isFile()) return;

        zos.putNextEntry(new ZipEntry(entryName));
        try (InputStream in = new FileInputStream(f)) {
            in.transferTo(zos);
        }
        zos.closeEntry();
    }

    private static void safeDelete(String path) {
        if (path == null) return;
        try { Files.deleteIfExists(Paths.get(path)); } catch (Exception ignored) {}
    }

    private static void safeDeleteParent(String path) {
        if (path == null) return;
        try {
            Path p = Paths.get(path).getParent();
            if (p != null) Files.deleteIfExists(p);
        } catch (Exception ignored) {}
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}
