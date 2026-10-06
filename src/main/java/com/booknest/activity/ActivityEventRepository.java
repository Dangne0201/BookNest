package com.booknest.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

	@Query(
			value = """
					select event from ActivityEvent event
					where (:patron = false or event.patronAccountId = :patronAccountId)
					and (:type is null or event.type = :type)
					and (:query = ''
						or lower(event.actorUsername) like lower(concat('%', :query, '%'))
						or lower(event.memberName) like lower(concat('%', :query, '%'))
						or lower(event.bookTitle) like lower(concat('%', :query, '%')))
					""",
			countQuery = """
					select count(event) from ActivityEvent event
					where (:patron = false or event.patronAccountId = :patronAccountId)
					and (:type is null or event.type = :type)
					and (:query = ''
						or lower(event.actorUsername) like lower(concat('%', :query, '%'))
						or lower(event.memberName) like lower(concat('%', :query, '%'))
						or lower(event.bookTitle) like lower(concat('%', :query, '%')))
					"""
	)
	Page<ActivityEvent> search(
			@Param("patron") boolean patron,
			@Param("patronAccountId") Long patronAccountId,
			@Param("type") ActivityEventType type,
			@Param("query") String query,
			Pageable pageable
	);
}
