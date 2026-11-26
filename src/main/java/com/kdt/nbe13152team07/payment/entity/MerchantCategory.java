package com.kdt.nbe13152team07.payment.entity;

public enum MerchantCategory {
    UTILITY("1111", "UTILITY"),
    RENT("2222", "RENT"),
    TELECOM("3333", "TELECOM"),
    TRANSPORT("4444", "TRANSPORT"),
    RESTAURANT("5812", "FOOD"),
    CAFE("5814", "CAFE"),
    SHOPPING("5311", "SHOPPING"),
    LIVING("5411", "LIVING");

    private final String mcc;
    private final String categoryCode;

    MerchantCategory(String mcc, String categoryCode) {
        this.mcc = mcc;
        this.categoryCode = categoryCode;
    }

    public static String findCategoryCode(String mcc){
        for(MerchantCategory category : values()){
            if (category.mcc.equals(mcc)){
                return category.categoryCode;
            }
        }
        return "ETC";
    }
}