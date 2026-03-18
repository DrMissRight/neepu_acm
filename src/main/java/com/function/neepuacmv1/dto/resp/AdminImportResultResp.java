package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminImportResultResp {
    private Integer totalRows;
    private Integer successRows;
    private Integer failRows;
    /**
     * 每条失败原因：第几行 + 原因
     */
    private List<String> failReasons;
}
