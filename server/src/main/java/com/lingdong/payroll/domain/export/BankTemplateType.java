package com.lingdong.payroll.domain.export;

public enum BankTemplateType {
    CCB("建设银行", "templates/bank/ccb-payroll.xlsx", ".xlsx"),
    BOC("中国银行", "templates/bank/boc-payroll.xls", ".xls");

    private final String displayName;
    private final String templatePath;
    private final String extension;

    BankTemplateType(String displayName, String templatePath, String extension) {
        this.displayName = displayName;
        this.templatePath = templatePath;
        this.extension = extension;
    }

    public String displayName() {
        return displayName;
    }

    public String templatePath() {
        return templatePath;
    }

    public String extension() {
        return extension;
    }
}
