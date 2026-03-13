package com.example.shop.hellper;

public final class TextNormalizer {
    public static String normalizeName(String input){
        if (input == null) return null;
        input = input.trim().toLowerCase();
        // dùng replaceAll để xóa ký tự không phải chữ/số/khoảng trắng
        input = input.replaceAll("[^\\p{L}\\p{N}\\s]+", "");
        // optional: collapse multiple spaces
        input = input.replaceAll("\\s+", " ");
        return input;
    }
    public static String normalizeDescriptionAndNotification(String input) {
        if (input == null || input.isBlank()) return "";

        input = input.trim();

        // Split câu dựa trên ., !, ?
        String[] sentences = input.split("(?<=[.!?])");
        StringBuilder result = new StringBuilder();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            // Viết hoa chữ đầu, giữ phần còn lại nguyên gốc
            String formatted = sentence.substring(0,1).toUpperCase() + sentence.substring(1);
            result.append(formatted).append(" "); // thêm khoảng trắng giữa các câu
        }

        String output = result.toString().trim();

        // Thêm dấu chấm nếu kết thúc không có dấu ., !, ?
        if (!output.endsWith(".") && !output.endsWith("!") && !output.endsWith("?")) {
            output += ".";
        }

        return output;
    }
    public static String toUSD(Double amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Giá tiền không được null");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Giá tiền không được âm");
        }
        return String.format("%.2f USD", amount);
    }
    public static String normalizeToken(String pre,String current){
        if(pre==null||pre.isEmpty()) return current;
        char lastChar=pre.charAt(pre.length()-1);
        char firstChar=current.charAt(0);
        if(Character.isLetterOrDigit(lastChar)
        && Character.isLetterOrDigit(firstChar)){
            return " "+current;
        }
        return current;
    }


}
