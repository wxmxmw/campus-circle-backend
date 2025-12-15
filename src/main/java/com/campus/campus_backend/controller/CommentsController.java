package com.campus.campus_backend.controller;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.common.Result;
import com.campus.campus_backend.domain.Comment;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.CommentLikeRepository;
import com.campus.campus_backend.repository.CommentRepository;
import com.campus.campus_backend.repository.UserRepository;
import com.campus.campus_backend.repository.PostRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentsController {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public CommentsController(CommentRepository commentRepository, CommentLikeRepository commentLikeRepository,
            UserRepository userRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @PostMapping("/{id}/like")
    public Result<java.util.Map<String, Boolean>> like(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id, @RequestParam(required = false) Boolean isLike) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        boolean likeAction = isLike == null ? true : isLike;
        boolean existed = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId());
        if (likeAction && !existed) {
            com.campus.campus_backend.domain.CommentLike cl = new com.campus.campus_backend.domain.CommentLike();
            cl.setComment(comment);
            cl.setUser(user);
            commentLikeRepository.save(cl);
        } else if (!likeAction && existed) {
            commentLikeRepository.findByCommentIdAndUserId(comment.getId(), user.getId())
                    .ifPresent(commentLikeRepository::delete);
        }
        long count = commentLikeRepository.countByCommentId(comment.getId());
        comment.setLikeCount((int) count);
        commentRepository.save(comment);
        boolean isLiked = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId());
        java.util.Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("isLiked", isLiked);
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        Comment c = commentRepository.findById(id).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!isAdmin && (c.getUser() == null || !c.getUser().getId().equals(user.getId()))) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        var post = c.getPost();
        commentRepository.delete(c);
        if (post != null) {
            int current = post.getCommentCount() != null ? post.getCommentCount() : 0;
            post.setCommentCount(Math.max(0, current - 1));
            postRepository.save(post);
        }
        return Result.okMessage("删除成功");
    }
}
