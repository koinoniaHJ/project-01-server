package com.erp.server.sales.shipment.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// ********** 유효 납품서 데이터를 판매 금액 없이 한글 A4 PDF로 생성하기 위한 Service 클래스 **********
@Service
public class DeliveryNotePdfService {

	private static final float PAGE_MARGIN = 36f;
	private static final float PAGE_BOTTOM = 48f;
	private static final float TABLE_ROW_HEIGHT = 24f;
	private static final float TABLE_WIDTH = 523f;
	private static final float[] TABLE_COLUMN_WIDTHS = { 28f, 68f, 118f, 42f, 90f, 85f, 92f };
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final Path fontPath;

	// ========== 발주서와 동일한 환경 설정을 사용하여 한글 TTF 글꼴 경로를 구성하는 생성자 ==========
	public DeliveryNotePdfService(@Value("${erp.document.font-path:C:/Windows/Fonts/malgun.ttf}") String fontPath) {
		this.fontPath = Path.of(fontPath);
	}

	// ========== 납품서 기본정보와 포장 LOT를 A4 PDF byte 배열로 생성하는 메서드 ==========
	public byte[] createDeliveryNotePdf(DeliveryNoteDocumentData data) {
		validateFontFile();
		try (PDDocument document = new PDDocument();
				InputStream fontInputStream = Files.newInputStream(fontPath);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PDType0Font font = PDType0Font.load(document, fontInputStream);
			PageContext context = createPage(document);
			try {
				writeHeader(context, font, data);
				writeTableHeader(context, font);
				for (DeliveryNoteDocumentData.Item item : data.items()) {
					if (context.y < PAGE_BOTTOM + TABLE_ROW_HEIGHT) {
						context.contentStream.close();
						context = createPage(document);
						writeTableHeader(context, font);
					}
					writeItemRow(context, font, item);
				}
				writeFooter(context, font, data);
			} finally {
				context.contentStream.close();
			}
			document.save(outputStream);
			return outputStream.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("납품서 PDF를 생성하지 못했습니다.", exception);
		}
	}

	private void validateFontFile() {
		if (!Files.isRegularFile(fontPath)) {
			throw new IllegalStateException("납품서 PDF용 한글 글꼴 파일을 찾을 수 없습니다: " + fontPath);
		}
	}

	private PageContext createPage(PDDocument document) throws IOException {
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		return new PageContext(document, new PDPageContentStream(document, page),
				PDRectangle.A4.getHeight() - PAGE_MARGIN);
	}

	// ========== 납품서 제목·번호·발행·주문·거래처·배송·출고 창고 정보를 출력하는 메서드 ==========
	private void writeHeader(PageContext context, PDType0Font font, DeliveryNoteDocumentData data) throws IOException {
		writeCenteredText(context, font, 20f, "납 품 서", context.y);
		context.y -= 38f;
		writeText(context, font, 9f, PAGE_MARGIN, context.y,
				"납품서 번호: DN-" + data.shipmentId() + "-" + data.issueSequence());
		writeText(context, font, 9f, 330f, context.y, "발행 일시: " + formatDateTime(data.issuedAt()));
		context.y -= 19f;
		writeText(context, font, 9f, PAGE_MARGIN, context.y, "주문 번호: " + data.salesOrderId());
		writeText(context, font, 9f, 330f, context.y,
				"출고 창고: " + data.warehouseName() + " (" + data.warehouseCode() + ")");
		context.y -= 19f;
		writeText(context, font, 9f, PAGE_MARGIN, context.y,
				"거래처: " + data.customerName() + " (" + data.customerCode() + ")");
		context.y -= 19f;
		writeText(context, font, 9f, PAGE_MARGIN, context.y,
				"배송지: " + fitText(font, 9f, joinAddress(data.deliveryAddress(), data.deliveryAddressDetail()), 515f));
		context.y -= 19f;
		writeText(context, font, 9f, PAGE_MARGIN, context.y,
				"수령인: " + data.recipientName() + " / " + data.recipientPhone());
		context.y -= 28f;
	}

	private void writeTableHeader(PageContext context, PDType0Font font) throws IOException {
		String[] headers = { "No", "품목 코드", "품목명", "단위", "LOT 번호", "사용기한", "출고 수량" };
		writeTableRow(context, font, headers, true);
	}

	private void writeItemRow(PageContext context, PDType0Font font, DeliveryNoteDocumentData.Item item)
			throws IOException {
		String[] values = { String.valueOf(item.lineNo()), item.itemCode(), item.itemName(), item.unit(),
				item.lotNumber(), item.expiryDate() == null ? "-" : item.expiryDate().format(DATE_FORMATTER),
				formatQuantity(item.packedQuantity()) };
		writeTableRow(context, font, values, false);
	}

	private void writeTableRow(PageContext context, PDType0Font font, String[] values, boolean header)
			throws IOException {
		float rowTop = context.y;
		drawTableRowBorder(context, rowTop);
		float x = PAGE_MARGIN;
		for (int index = 0; index < values.length; index++) {
			boolean center = header || index == 0 || index == 3 || index >= 5;
			writeCellText(context, font, 8f, values[index], x, rowTop, TABLE_COLUMN_WIDTHS[index], center);
			x += TABLE_COLUMN_WIDTHS[index];
		}
		context.y -= TABLE_ROW_HEIGHT;
	}

	private void writeFooter(PageContext context, PDType0Font font, DeliveryNoteDocumentData data) throws IOException {
		BigDecimal totalQuantity = data.items().stream().map(DeliveryNoteDocumentData.Item::packedQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		context.y -= 18f;
		writeText(context, font, 10f, 420f, context.y, "총 출고 수량: " + formatQuantity(totalQuantity));
	}

	private void drawTableRowBorder(PageContext context, float rowTop) throws IOException {
		PDPageContentStream stream = context.contentStream;
		float rowBottom = rowTop - TABLE_ROW_HEIGHT;
		stream.setLineWidth(0.5f);
		stream.addRect(PAGE_MARGIN, rowBottom, TABLE_WIDTH, TABLE_ROW_HEIGHT);
		float x = PAGE_MARGIN;
		for (int index = 0; index < TABLE_COLUMN_WIDTHS.length - 1; index++) {
			x += TABLE_COLUMN_WIDTHS[index];
			stream.moveTo(x, rowBottom);
			stream.lineTo(x, rowTop);
		}
		stream.stroke();
	}

	private void writeCellText(PageContext context, PDType0Font font, float fontSize, String value,
			float x, float rowTop, float cellWidth, boolean center) throws IOException {
		String fitted = fitText(font, fontSize, value, cellWidth - 8f);
		float width = getTextWidth(font, fontSize, fitted);
		float textX = center ? x + Math.max((cellWidth - width) / 2f, 4f) : x + 4f;
		writeText(context, font, fontSize, textX, rowTop - 16f, fitted);
	}

	private void writeCenteredText(PageContext context, PDType0Font font, float fontSize, String value, float y)
			throws IOException {
		writeText(context, font, fontSize, (PDRectangle.A4.getWidth() - getTextWidth(font, fontSize, value)) / 2f,
				y, value);
	}

	private void writeText(PageContext context, PDType0Font font, float fontSize, float x, float y, String value)
			throws IOException {
		context.contentStream.beginText();
		context.contentStream.setFont(font, fontSize);
		context.contentStream.newLineAtOffset(x, y);
		context.contentStream.showText(value == null ? "" : value);
		context.contentStream.endText();
	}

	private String fitText(PDType0Font font, float fontSize, String value, float maxWidth) throws IOException {
		String text = value == null ? "" : value;
		if (getTextWidth(font, fontSize, text) <= maxWidth) return text;
		StringBuilder fitted = new StringBuilder(text);
		while (!fitted.isEmpty() && getTextWidth(font, fontSize, fitted + "…") > maxWidth) {
			fitted.deleteCharAt(fitted.length() - 1);
		}
		return fitted + "…";
	}

	private float getTextWidth(PDType0Font font, float fontSize, String value) throws IOException {
		return font.getStringWidth(value) / 1000f * fontSize;
	}

	private String joinAddress(String address, String detail) {
		return (address == null ? "" : address) + (detail == null || detail.isBlank() ? "" : " " + detail);
	}

	private String formatDateTime(java.time.LocalDateTime value) {
		return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
	}

	private String formatQuantity(BigDecimal value) {
		return value == null ? "0" : value.stripTrailingZeros().toPlainString();
	}

	private static class PageContext {
		private final PDDocument document;
		private PDPageContentStream contentStream;
		private float y;

		private PageContext(PDDocument document, PDPageContentStream contentStream, float y) {
			this.document = document;
			this.contentStream = contentStream;
			this.y = y;
		}
	}
}
