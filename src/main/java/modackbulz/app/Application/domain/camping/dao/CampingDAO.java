package modackbulz.app.Application.domain.camping.dao;

import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CampingDAO {
  /**
   * 캠핑장 정보를 DB에 저장하거나 이미 있으면 업데이트합니다.
   * @param item 캠핑장 정보 DTO
   */
  void saveOrUpdate(GoCampingDto.Item item);

  /**
   * 전체 목록을 가져오기 위한 매서드
   * @return List<CampNm>
   */
  List<CampNm> findAllForSync();

  /**
   * contentId로 DB에서 캠핑장 정보를 찾습니다.
   * @param contentId 캠핑장 콘텐츠 ID
   * @return Optional<GoCampingDto.Item>
   */
  Optional<GoCampingDto.Item> findByContentId(Long contentId);

  /**
   * [추가됨] DB에 저장된 모든 캠핑장 목록을 페이징하여 조회합니다.
   * @param pageable 페이징 정보
   * @return Page<GoCampingDto.Item>
   */
  Page<GoCampingDto.Item> findAll(Pageable pageable);

  /**
   * [추가됨] 키워드로 캠핑장을 검색하고 페이징하여 조회합니다.
   * @param keyword 검색 키워드
   * @param pageable 페이징 정보
   * @return Page<GoCampingDto.Item>
   */
  Page<GoCampingDto.Item> search(String keyword, Pageable pageable);

  /**
   * 스크랩 카운트
   * @param pageable 페이징 정보
   * @return
   */
  Page<GoCampingDto.Item> findAllOrderByScrapCountDesc(Pageable pageable);

  /**
   * 지역으로 필터링하고 스크랩 순으로 정렬하여 캠핑장 목록을 조회합니다.
   * @param region 검색할 지역 키워드
   * @param pageable 페이징 정보
   * @return Page<GoCampingDto.Item>
   */
  Page<GoCampingDto.Item> findAllByRegionOrderByScrapCountDesc(String region, Pageable pageable);
}
