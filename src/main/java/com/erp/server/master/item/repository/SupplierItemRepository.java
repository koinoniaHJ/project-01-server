package com.erp.server.master.item.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.master.item.domain.SupplierItem;

// ********** SUPPLIER_ITEM의 관계 등록·해제와 품목별 취급 공급업체 조회를 처리하기 위한 Repository interface **********
public interface SupplierItemRepository extends JpaRepository<SupplierItem, Long> {

	// ========== 지정한 품목의 취급 공급업체 관계를 공급업체 코드 오름차순으로 조회하는 메서드 ==========
	// 품목 상세 응답에서 공급업체 정보를 사용할 수 있도록 SUPPLIER를 함께 조회한다.
	@Query("""
			select si
			from SupplierItem si
			join fetch si.supplier s
			where si.item.itemId = :itemId
			order by s.supplierCode asc
			""")
	List<SupplierItem> findAllByItemId(@Param("itemId") Long itemId);

	// ========== 동일한 품목과 공급업체의 취급 관계가 이미 존재하는지 확인하는 메서드 ==========
	boolean existsByItemItemIdAndSupplierSupplierId(Long itemId, Long supplierId);

	// ========== 품목과 공급업체 식별자로 관계 해제 대상 SUPPLIER_ITEM을 조회하는 메서드 ==========
	Optional<SupplierItem> findByItemItemIdAndSupplierSupplierId(Long itemId, Long supplierId);
}
