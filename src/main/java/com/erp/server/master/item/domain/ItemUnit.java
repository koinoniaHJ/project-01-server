package com.erp.server.master.item.domain;

// ********** ITEM.unit에서 사용할 품목 기준 재고 단위를 Java와 DB에서 같은 값으로 관리하기 위한 enum **********
public enum ItemUnit {

	G,		// 그램
	KG,		// 킬로그램
	EA,		// 개
	PACK,	// 팩
	BOX,	// 박스
	OTHER	// 기타 단위
}
