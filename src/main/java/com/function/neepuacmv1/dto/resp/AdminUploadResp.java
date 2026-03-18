package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 上传结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUploadResp {
    private Integer successCount;
    private Integer failCount;
    private List<String> failReasons;
}
