package org.com.stocknote.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.stocknote.domain.hashtag.entity.Hashtag;
import org.com.stocknote.domain.hashtag.service.HashtagService;
import org.com.stocknote.domain.like.repository.LikeRepository;
import org.com.stocknote.domain.member.entity.Member;
import org.com.stocknote.domain.notification.repository.CommentNotificationRepository;
import org.com.stocknote.domain.notification.repository.KeywordNotificationRepository;
import org.com.stocknote.domain.post.dto.PostModifyDto;
import org.com.stocknote.domain.post.dto.PostResponseDto;
import org.com.stocknote.domain.post.dto.PostSearchConditionDto;
import org.com.stocknote.domain.post.entity.Post;
import org.com.stocknote.domain.post.entity.PostCategory;
import org.com.stocknote.domain.post.dto.*;
import org.com.stocknote.domain.post.repository.PostRepository;
import org.com.stocknote.domain.post.repository.PostSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final HashtagService hashtagService;
    private final CommentNotificationRepository commentNotificationRepository;
    private final KeywordNotificationRepository keywordNotificationRepository;
    private final LikeRepository likeRepository;
    @Autowired
    private final PostSearchRepository postSearchRepository;
    private static final String POPULAR_POSTS_CACHE_KEY = "popularPosts";
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public Post createPost(PostResponseDto postResponseDto, Member member) {

        Post post = postResponseDto.toEntity(member);
        Post savedPost = postRepository.save(post);

        hashtagService.createHashtags(savedPost.getId(), postResponseDto.hashtags());
        return post;
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.map(post -> {
            List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                    .stream()
                    .map(Hashtag::getName)
                    .toList();
            return PostResponseDto.fromPost(post, hashtags);
        });
    }

    @Transactional(readOnly=true)
    public Page<PostResponseDto> getPostsByCategory(PostCategory category, Pageable pageable) {
        Page<Post> posts = postRepository.findByCategory(category, pageable);  // Pageable 추가
        return posts.map(post -> {
            List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                    .stream()
                    .map(Hashtag::getName)
                    .toList();
            return PostResponseDto.fromPost(post, hashtags);

        });
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                .stream()
                .map(Hashtag::getName)
                .toList();
        return PostResponseDto.fromPost(post, hashtags);
    }

    @Transactional
    public void updatePost(Long id, PostModifyDto postModifyDto) {
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        existingPost.bodyUpdate(postModifyDto.getContent());
        existingPost.titleUpdate(postModifyDto.getTitle());
        existingPost.categoryUpdate(PostCategory.valueOf(postModifyDto.getCategory()));
        postRepository.save(existingPost);

        hashtagService.updateHashtags(id, postModifyDto.getHashtags());
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        likeRepository.deleteByPostId(id);
        hashtagService.deleteHashtagsByPostId(id);
        commentNotificationRepository.deleteByRelatedPostId(id);
        keywordNotificationRepository.deleteByRelatedPostId(id);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPopularPosts(Pageable pageable) {
        String cacheKey = POPULAR_POSTS_CACHE_KEY + ":" + pageable.getPageNumber();

        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData instanceof Page) {
            return (Page<PostResponseDto>) cachedData;
        }

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        Page<Post> popularPosts = postRepository.findPopularPosts(threeDaysAgo, pageable);

        List<PostResponseDto> sortedPosts = popularPosts.stream()
                .map(post -> {
                    List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                            .stream()
                            .map(Hashtag::getName)
                            .toList();
                    return PostResponseDto.fromPost(post, hashtags);
                })
                .collect(Collectors.toList());

        Page<PostResponseDto> response = new PageImpl<>(sortedPosts, pageable, popularPosts.getTotalElements());

        redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(5));

        return response;
    }

    // 좋아요 순 조회
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPopularPostsByLikes(Pageable pageable) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Page<Post> popularPosts = postRepository.findPopularPostsByLikes(sevenDaysAgo, pageable);

        return getPostResponseDtos(pageable, popularPosts);
    }

    // 댓글 순 조회
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPopularPostsByComments(Pageable pageable) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Page<Post> popularPosts = postRepository.findPopularPostsByComments(sevenDaysAgo, pageable);

        return getPostResponseDtos(pageable, popularPosts);
    }

    // 검색 기능
    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchPosts(PostSearchConditionDto condition, Pageable pageable) {
        Page<Post> searchResults = postSearchRepository.search(condition, pageable);  // postRepositoryCustom -> postSearchRepository

        return searchResults.map(post -> {
            List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                    .stream()
                    .map(Hashtag::getName)
                    .toList();
            return PostResponseDto.fromPost(post, hashtags);
        });
    }

    public Page<PostStockResponse> getPostsByStockName(String sName, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return postRepository.findByHashtagNameOrderByCreatedAtDesc(sName, pageRequest)
                .map(PostStockResponse::from);
    }
    private Page<PostResponseDto> getPostResponseDtos (Pageable pageable, Page<Post> popularPosts) {
        List<PostResponseDto> sortedPosts = popularPosts.stream()
                .map(post -> {
                    List<String> hashtags = hashtagService.getHashtagsByPostId(post.getId())
                            .stream()
                            .map(Hashtag::getName)
                            .toList();
                    return PostResponseDto.fromPost(post, hashtags);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(sortedPosts, pageable, popularPosts.getTotalElements());
    }
}
