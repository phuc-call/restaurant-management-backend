package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;


import com.example.shop.exception.APIException;

import com.example.shop.hellper.BillHellper;


import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartNoteRequest;

import com.example.shop.repository.CartItemRepo;
import com.example.shop.repository.CartRepo;
import com.example.shop.service.CartItemService;
import com.example.shop.service.CartService;
import com.lowagie.text.*;


import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Map;


@Transactional
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartItemRepo cartItemRepo;
    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    CartRepo cartRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    CartItemService cartItemService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public CartDTO addToCart(Long tableId, Long menuItemId, int quantity) {

        // Lấy cart theo bàn
        Cart cartDB = cartRepo.findByRestaurantTable_tableId(tableId)
                .orElseThrow(() -> new APIException("Table has not cart!!"));
        if (quantity <= 0) {
            throw new APIException("Số lượng món phải lớn hơn 0");
        }

        // Add hoặc update quantity
        CartItem savedItem = cartItemService.addOrUpdateItem(
                cartDB,
                menuItemId,
                quantity
        );


        // Tính tổng tiền giỏ hàng
        BigDecimal total = cartDB.getCartItems()
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new APIException("Tổng tiền giỏ thành không hợp lệ");
        }

        cartDB.setTotalPrice(total);

        cartRepo.save(cartDB);

        CartDTO cartDTO = modelMapper.map(cartDB, CartDTO.class);

        Long tableIdRealtime = cartDB.getRestaurantTable().getTableId();
        messagingTemplate.convertAndSend(
                "/topic/cart/" + tableIdRealtime,
                cartDTO
        );


        messagingTemplate.convertAndSend(
                "/topic/cashier",
                Map.of(
                        "event", "CART_UPDATED",
                        "data", cartDTO
                )
        );

        // trả về DTO
        return cartDTO;
    }

    @Override
    public CartDTO getCartById(Long cartId) {
        Cart cart = cartRepo.findById(cartId).orElseThrow(() ->
                new APIException("Not found cart!"));
        return modelMapper.map(cart, CartDTO.class);
    }

    @Override
    public CartNoteRequest noteCart(String note, Long cartId) {
        Cart cart = cartRepo.findById(cartId).orElseThrow(() -> new APIException("Không tìm thấy cart"));
        cart.setNoteCart(note);
        cartRepo.save(cart);
        return modelMapper.map(cart, CartNoteRequest.class);
    }

    @Override
    public byte[] generateBillPdf(Long cartId) {

        Cart cart = cartRepo.findById(cartId).orElseThrow(() -> new APIException("Không tìm thấy cart"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A5, 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            Font normal = new Font(Font.HELVETICA, 9);
            Font bold = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font title = new Font(Font.HELVETICA, 12, Font.BOLD);

            // ===== HEADER =====
            Paragraph shop = new Paragraph("NHÀ HÀNG Hồng Phúc", bold);
            shop.setAlignment(Element.ALIGN_CENTER);
            document.add(shop);

            document.add(new Paragraph("23/57 Thái Thịnh,Phước Long B, Thành Phố Hồ Chí Minh", normal));
            document.add(new Paragraph("0327568095", normal));

            Paragraph billTitle = new Paragraph("HÓA ĐƠN TÍNH TẠM", title);
            billTitle.setAlignment(Element.ALIGN_CENTER);
            billTitle.setSpacingBefore(8);
            document.add(billTitle);

            document.add(new Paragraph("Bàn số: " + cart.getRestaurantTable().getTableName(), normal));
            document.add(Chunk.NEWLINE);

            // ===== INFO =====
            document.add(new Paragraph("Ngày: " + cart.getUpdatedAt(), normal));
            document.add(Chunk.NEWLINE);

            // ===== LINE =====
            document.add(new LineSeparator());

            // ===== ITEMS TABLE =====
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1, 2, 2});
            BillHellper billHellper = new BillHellper();
            billHellper.addCell(table, "Tên món", bold, Element.ALIGN_LEFT);
            billHellper.addCell(table, "SL", bold, Element.ALIGN_CENTER);
            billHellper.addCell(table, "Đơn giá", bold, Element.ALIGN_RIGHT);
            billHellper.addCell(table, "Thành tiền", bold, Element.ALIGN_RIGHT);

            document.add(new LineSeparator());

            for (CartItem item : cart.getCartItems()) {
                billHellper.addCell(table, item.getMenuItem().getName(), normal, Element.ALIGN_LEFT);
                billHellper.addCell(table, String.valueOf(item.getQuantity()), normal, Element.ALIGN_CENTER);
                billHellper.addCell(table, billHellper.formatMoney(item.getUnitPrice()), normal, Element.ALIGN_RIGHT);
                billHellper.addCell(table, billHellper.formatMoney(item.getTotalPrice()), normal, Element.ALIGN_RIGHT);
            }

            document.add(table);
            document.add(new LineSeparator());

            // ===== TOTAL =====
            document.add(billHellper.totalLine("Thành tiền", cart.getTotalPrice(), normal));
            document.add(Chunk.NEWLINE);

            // ===== FOOTER =====
            Paragraph thanks = new Paragraph("Trân trọng cảm ơn!", bold);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            Paragraph note = new Paragraph("(Hóa đơn chưa bao gồm thuế GTGT)", normal);
            note.setAlignment(Element.ALIGN_CENTER);
            document.add(note);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Generate PDF failed", e);
        }
    }

}
