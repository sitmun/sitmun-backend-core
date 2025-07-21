package org.sitmun.domain.comment;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "comment")
@RepositoryRestResource(collectionResourceRel = "comments", path = "comments")
public interface CommentRepository extends org.springframework.data.jpa.repository.JpaRepository<Comment, Integer> {
}
