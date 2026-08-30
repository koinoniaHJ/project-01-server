package com.erp.server.purchase.order.mail;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.erp.server.purchase.order.document.PurchaseOrderDocumentData;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

// ********** Spring Mail의 JavaMailSender로 발주서 PDF가 첨부된 MIME 이메일을 외부 SMTP 서버에 전달하기 위한 Component 클래스 **********
@Component
public class PurchaseOrderMailSender {

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final String fromAddress;

	// ========== SMTP 설정이 없는 개발 환경에서도 서버가 시작되도록 JavaMailSender를 지연 조회하는 생성자 ==========
	public PurchaseOrderMailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
			@Value("${erp.mail.from:${spring.mail.username:}}") String fromAddress) {
		this.mailSenderProvider = mailSenderProvider;
		this.fromAddress = fromAddress;
	}

	// ========== 공급업체 이메일로 발주 안내 본문과 PDF 첨부파일을 전송하는 메서드 ==========
	public void sendPurchaseOrder(PurchaseOrderDocumentData documentData, byte[] pdfBytes) {
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

		if (mailSender == null) {
			throw new IllegalStateException("SMTP 설정이 없어 JavaMailSender를 사용할 수 없습니다.");
		}

		if (fromAddress == null || fromAddress.isBlank()) {
			throw new IllegalStateException("발신 이메일 주소가 설정되지 않았습니다.");
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

			helper.setFrom(fromAddress);
			helper.setTo(documentData.recipientEmail());
			helper.setSubject("[ERP] 발주서 안내 - 발주 번호 " + documentData.purchaseOrderId());
			helper.setText(createMailBody(documentData), false);
			helper.addAttachment(createAttachmentFileName(documentData), new ByteArrayResource(pdfBytes),
					"application/pdf");

			mailSender.send(message);

		} catch (MessagingException exception) {
			throw new IllegalStateException("발주서 이메일 메시지를 생성하지 못했습니다.", exception);
		}
	}

	// ========== 공급업체에 표시할 발주서 이메일 본문을 생성하는 메서드 ==========
	private String createMailBody(PurchaseOrderDocumentData documentData) {
		return """
				안녕하세요.

				%s 앞으로 발주서를 보내드립니다.
				첨부된 PDF 발주서를 확인해 주세요.

				발주 번호: %s
				공급업체 코드: %s

				감사합니다.
				""".formatted(documentData.supplierName(), documentData.purchaseOrderId(),
					documentData.supplierCode());
	}

	// ========== 발주 식별자를 포함한 PDF 첨부파일명을 생성하는 메서드 ==========
	private String createAttachmentFileName(PurchaseOrderDocumentData documentData) {
		return "purchase-order-" + documentData.purchaseOrderId() + ".pdf";
	}
}
