package com.example.shop.hellper;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.math.BigDecimal;

public class BillHellper {
    public void addCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3);
        table.addCell(cell);
    }
    public Paragraph totalLine(String label, BigDecimal value, Font font) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", font));
        p.add(new Chunk(formatMoney(value) + " đ", font));
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }
    public String formatMoney(BigDecimal money) {
        return String.format("%,.0f", money);
    }

}
