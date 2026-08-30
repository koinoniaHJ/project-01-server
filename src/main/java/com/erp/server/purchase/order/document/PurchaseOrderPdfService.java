package com.erp.server.purchase.order.document;

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

// ********** 발주 문서 데이터를 한글 글꼴이 포함된 A4 PDF 발주서로 생성하기 위한 Service 클래스 **********
@Service
public class PurchaseOrderPdfService {

	private static final float PAGE_MARGIN = 36f;
	private static final float PAGE_BOTTOM = 48f;
	private static final float TABLE_ROW_HEIGHT = 24f;
	private static final float TABLE_WIDTH = 523f;
	private static final float[] TABLE_COLUMN_WIDTHS = { 28f, 70f, 120f, 45f, 65f, 95f, 100f };
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final Path fontPath;

	// ========== 개발 환경 기본 글꼴과 운영 환경에서 덮어쓸 수 있는 한글 TTF 경로를 설정하는 생성자 ==========
	public PurchaseOrderPdfService(
			@Value("${erp.document.font-path:C:/Windows/Fonts/malgun.ttf}") String fontPath) {
		this.fontPath = Path.of(fontPath);
	}

	// ========== 발주 기본정보와 품목을 A4 PDF byte 배열로 생성하는 메서드 ==========
	public byte[] createPurchaseOrderPdf(PurchaseOrderDocumentData documentData) {
		validateFontFile();

		try (PDDocument document = new PDDocument();
				InputStream fontInputStream = Files.newInputStream(fontPath);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PDType0Font font = PDType0Font.load(document, fontInputStream);
			PageContext context = createPage(document);

			try {
				writeDocumentHeader(context, font, documentData);
				writeItemTableHeader(context, font);

				for (PurchaseOrderDocumentData.Item item : documentData.items()) {
					if (context.y < PAGE_BOTTOM + TABLE_ROW_HEIGHT) {
						context.contentStream.close();
						context = createPage(document);
						writeItemTableHeader(context, font);
					}

					writeItemRow(context, font, item);
				}

				writeDocumentFooter(context, font, documentData);
			} finally {
				context.contentStream.close();
			}

			document.save(outputStream);

			return outputStream.toByteArray();

		} catch (IOException exception) {
			throw new IllegalStateException("발주서 PDF를 생성하지 못했습니다.", exception);
		}
	}

	// ========== PDF에 포함할 한글 TTF 파일이 실제로 존재하는지 확인하는 메서드 ==========
	private void validateFontFile() {
		if (!Files.isRegularFile(fontPath)) {
			throw new IllegalStateException("발주서 PDF용 한글 글꼴 파일을 찾을 수 없습니다: " + fontPath);
		}
	}

	// ========== A4 페이지와 페이지에 쓸 ContentStream을 생성하는 메서드 ==========
	private PageContext createPage(PDDocument document) throws IOException {
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);

		return new PageContext(document, new PDPageContentStream(document, page),
				PDRectangle.A4.getHeight() - PAGE_MARGIN);
	}

	// ========== 발주서 제목·발주 식별자·공급업체·확정 일시·수신 주소를 출력하는 메서드 ==========
	private void writeDocumentHeader(PageContext context, PDType0Font font,
			PurchaseOrderDocumentData documentData) throws IOException {
		writeCenteredText(context, font, 20f, "발 주 서", context.y);
		context.y -= 38f;

		writeText(context, font, 10f, PAGE_MARGIN, context.y,
				"발주 번호: " + documentData.purchaseOrderId());
		writeText(context, font, 10f, 330f, context.y,
				"발주 확정: " + formatDateTime(documentData.orderedAt()));
		context.y -= 20f;

		writeText(context, font, 10f, PAGE_MARGIN, context.y,
				"공급업체: " + documentData.supplierName() + " (" + documentData.supplierCode() + ")");
		context.y -= 20f;

		writeText(context, font, 10f, PAGE_MARGIN, context.y,
				"수신 이메일: " + documentData.recipientEmail());
		context.y -= 28f;
	}

	// ========== 발주 품목 Table의 컬럼명과 테두리를 출력하는 메서드 ==========
	private void writeItemTableHeader(PageContext context, PDType0Font font) throws IOException {
		String[] headers = { "No", "품목 코드", "품목명", "단위", "수량", "단가", "금액" };
		float rowTop = context.y;

		drawTableRowBorder(context, rowTop);

		float x = PAGE_MARGIN;
		for (int index = 0; index < headers.length; index++) {
			writeCellText(context, font, 8f, headers[index], x, rowTop, TABLE_COLUMN_WIDTHS[index], true);
			x += TABLE_COLUMN_WIDTHS[index];
		}

		context.y -= TABLE_ROW_HEIGHT;
	}

	// ========== 한 발주 품목의 순번·품목·단위·수량·단가·금액을 Table 행으로 출력하는 메서드 ==========
	private void writeItemRow(PageContext context, PDType0Font font, PurchaseOrderDocumentData.Item item)
			throws IOException {
		String[] values = {
				String.valueOf(item.lineNo()), item.itemCode(), item.itemName(), item.unitName(),
				formatQuantity(item.orderedQuantity()), formatMoney(item.unitPrice()), formatMoney(item.lineAmount())
		};
		float rowTop = context.y;

		drawTableRowBorder(context, rowTop);

		float x = PAGE_MARGIN;
		for (int index = 0; index < values.length; index++) {
			boolean center = index == 0 || index == 3;
			writeCellText(context, font, 8f, values[index], x, rowTop, TABLE_COLUMN_WIDTHS[index], center);
			x += TABLE_COLUMN_WIDTHS[index];
		}

		context.y -= TABLE_ROW_HEIGHT;
	}

	// ========== 발주 총액과 메모를 품목 Table 아래에 출력하는 메서드 ==========
	private void writeDocumentFooter(PageContext context, PDType0Font font,
			PurchaseOrderDocumentData documentData) throws IOException {
		if (context.y < PAGE_BOTTOM + 70f) {
			context.contentStream.close();
			PageContext newContext = createPage(context.document);
			context.contentStream = newContext.contentStream;
			context.y = newContext.y;
		}

		context.y -= 18f;
		writeText(context, font, 11f, 385f, context.y,
				"총액: " + formatMoney(documentData.totalAmount()) + "원");
		context.y -= 24f;

		String memo = documentData.memo() == null ? "-" : documentData.memo();
		writeText(context, font, 9f, PAGE_MARGIN, context.y,
				"메모: " + fitText(font, 9f, memo, TABLE_WIDTH - 35f));
	}

	// ========== Table 행의 외곽선과 컬럼 구분선을 그리는 메서드 ==========
	private void drawTableRowBorder(PageContext context, float rowTop) throws IOException {
		PDPageContentStream contentStream = context.contentStream;
		float rowBottom = rowTop - TABLE_ROW_HEIGHT;

		contentStream.setLineWidth(0.5f);
		contentStream.addRect(PAGE_MARGIN, rowBottom, TABLE_WIDTH, TABLE_ROW_HEIGHT);

		float x = PAGE_MARGIN;
		for (int index = 0; index < TABLE_COLUMN_WIDTHS.length - 1; index++) {
			x += TABLE_COLUMN_WIDTHS[index];
			contentStream.moveTo(x, rowBottom);
			contentStream.lineTo(x, rowTop);
		}

		contentStream.stroke();
	}

	// ========== Table 셀 너비에 맞게 텍스트를 자르고 가운데 또는 왼쪽 정렬로 출력하는 메서드 ==========
	private void writeCellText(PageContext context, PDType0Font font, float fontSize, String value,
			float x, float rowTop, float cellWidth, boolean center) throws IOException {
		String fittedValue = fitText(font, fontSize, value, cellWidth - 8f);
		float textWidth = getTextWidth(font, fontSize, fittedValue);
		float textX = center ? x + Math.max((cellWidth - textWidth) / 2f, 4f) : x + 4f;
		float textY = rowTop - 16f;

		writeText(context, font, fontSize, textX, textY, fittedValue);
	}

	// ========== 페이지 가로 중앙을 기준으로 제목을 출력하는 메서드 ==========
	private void writeCenteredText(PageContext context, PDType0Font font, float fontSize, String value, float y)
			throws IOException {
		float textWidth = getTextWidth(font, fontSize, value);
		float x = (PDRectangle.A4.getWidth() - textWidth) / 2f;

		writeText(context, font, fontSize, x, y, value);
	}

	// ========== 지정한 좌표에 한 줄 텍스트를 출력하는 메서드 ==========
	private void writeText(PageContext context, PDType0Font font, float fontSize, float x, float y, String value)
			throws IOException {
		context.contentStream.beginText();
		context.contentStream.setFont(font, fontSize);
		context.contentStream.newLineAtOffset(x, y);
		context.contentStream.showText(value == null ? "" : value);
		context.contentStream.endText();
	}

	// ========== 셀 너비를 넘는 문자열의 뒤를 줄이고 말줄임표를 붙이는 메서드 ==========
	private String fitText(PDType0Font font, float fontSize, String value, float maxWidth) throws IOException {
		String text = value == null ? "" : value;

		if (getTextWidth(font, fontSize, text) <= maxWidth) {
			return text;
		}

		String ellipsis = "…";
		StringBuilder fitted = new StringBuilder(text);

		while (!fitted.isEmpty()
				&& getTextWidth(font, fontSize, fitted + ellipsis) > maxWidth) {
			fitted.deleteCharAt(fitted.length() - 1);
		}

		return fitted + ellipsis;
	}

	// ========== 현재 PDF 글꼴과 크기를 기준으로 문자열 너비를 계산하는 메서드 ==========
	private float getTextWidth(PDType0Font font, float fontSize, String value) throws IOException {
		return font.getStringWidth(value) / 1000f * fontSize;
	}

	// ========== 발주 확정 일시를 PDF 표시 형식으로 변환하는 메서드 ==========
	private String formatDateTime(java.time.LocalDateTime value) {
		return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
	}

	// ========== 발주 수량의 불필요한 소수점 0을 제거하여 표시하는 메서드 ==========
	private String formatQuantity(BigDecimal value) {
		return value == null ? "0" : value.stripTrailingZeros().toPlainString();
	}

	// ========== 단가·금액을 천 단위 구분과 소수 둘째 자리 형식으로 변환하는 메서드 ==========
	private String formatMoney(BigDecimal value) {
		// DecimalFormat은 Thread-safe하지 않으므로 Singleton Service의 공유 필드로 두지 않고 요청마다 생성한다.
		return new java.text.DecimalFormat("#,##0.00").format(value == null ? BigDecimal.ZERO : value);
	}

	// PDF 페이지별 출력 Stream과 다음 출력 Y 좌표를 함께 관리한다.
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
