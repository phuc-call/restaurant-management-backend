package com.example.shop.entity.enums;

public enum Floor {
    FLOOR_1("Tầng 1"),
    FLOOR_2("Tầng 2"),
    FLOOR_3("Tầng 3"),
    FLOOR_4("Tầng 4");

    private final String label;

    Floor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}