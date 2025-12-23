package com.example.demo.common;

import java.util.Set;

/**
 * 文档相关常量定义，确保后端实现与开发手册规范一致。
 */
public final class DocumentConstants {

    private DocumentConstants() {
    }

    public static final String DOC_TYPE_MARKDOWN = "markdown";
    public static final String DOC_TYPE_DOCX = "docx";
    public static final String DOC_TYPE_TXT = "txt";

    public static final Set<String> SUPPORTED_DOC_TYPES = Set.of(
        DOC_TYPE_MARKDOWN,
        DOC_TYPE_DOCX,
        DOC_TYPE_TXT
    );

    public static final String DEFAULT_DOC_TYPE = DOC_TYPE_MARKDOWN;

    public static final String EXPORT_FORMAT_PDF = "pdf";

    public static final Set<String> SUPPORTED_EXPORT_FORMATS = Set.of(
        DOC_TYPE_MARKDOWN,
        DOC_TYPE_DOCX,
        DOC_TYPE_TXT,
        EXPORT_FORMAT_PDF
    );
}
