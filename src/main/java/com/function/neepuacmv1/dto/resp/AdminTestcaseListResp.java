package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 测试点/样例点列表项 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTestcaseListResp {
    private Long id;
    private Long problemId;
    private Integer isSample;
    private Integer isPublic;
    private Integer score;
    private Integer orderIndex;
    private String inputPath;
    private String outputPath;
    private LocalDateTime updatedAt;
}
