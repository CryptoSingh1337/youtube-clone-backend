package com.saransh.vidflowservice.video.impl;

import com.saransh.vidflowdata.entity.Comment;
import com.saransh.vidflowdata.entity.User;
import com.saransh.vidflowdata.entity.Video;
import com.saransh.vidflowdata.entity.VideoStatus;
import com.saransh.vidflowdata.repository.VideoRepository;
import com.saransh.vidflownetwork.request.video.CommentRequestModel;
import com.saransh.vidflownetwork.request.video.UpdateCommentRequestModel;
import com.saransh.vidflownetwork.request.video.VideoMetadataRequestModel;
import com.saransh.vidflownetwork.response.video.*;
import com.saransh.vidflowservice.mapper.CommentMapper;
import com.saransh.vidflowservice.mapper.VideoMapper;
import com.saransh.vidflowservice.user.UserService;
import com.saransh.vidflowservice.video.VideoService;
import com.saransh.vidflowutilities.exceptions.MongoWriteException;
import com.saransh.vidflowutilities.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * author: CryptoSingh1337
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final UserService userService;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    
    private final int PAGE_OFFSET = 10;

    @Override
    public List<Video> getAllVideos(int page) {
        log.debug("Retrieving all videos");
        return videoRepository.findAll(getAllVideosPageRequest(page)).stream().toList();
    }

    @Override
    public List<VideoCardResponseModel> getAllTrendingVideos(int page) {
        log.debug("Retrieving all trending videos");
        return videoRepository.findAllByVideoStatusEquals(getAllTrendingVideosPageRequest(page),
                        VideoStatus.PUBLIC.name()).stream()
                .map(videoMapper::videoToVideoCard)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchVideoResponseModel> getAllSearchedVideos(String q, int page) {
        log.debug("Searching all the videos with title: {}", q);
        return videoRepository.findAllByTitleContainingIgnoreCase(getAllVideosPageRequest(page), q).stream()
                .map(videoMapper::videoToSearchVideoCard)
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoCardResponseModel> getAllVideosByUserId(String userId, int page) {
        log.debug("Retrieving all the video with userId: {}", userId);
        return videoRepository.findAllByUserId(getAllVideosPageRequest(page), userId).stream()
                .map(videoMapper::videoToVideoCard)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVideoCardResponseModel> getAllVideosByUsername(String username) {
        log.debug("Retrieving all the video with username: {}", username);
        User user = userService.findUserByUsername(username);
        return videoRepository.findAllByUserId(user.getId()).stream()
                .map(videoMapper::videoToUserVideoCard)
                .collect(Collectors.toList());
    }

    @Override
    public WatchVideoResponseModel getVideoById(String id) {
        log.debug("Retrieving video with ID: {}", id);
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        return videoMapper.videoToWatchVideo(video);
    }

    @Override
    @Transactional
    public void insert(String videoId, VideoMetadataRequestModel videoMetadata) {
        log.debug("Saving video metadata...");
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Video video = videoMapper.videoMetadataToVideo(videoMetadata);
        User user = userService.findUserByUsername(username);
        video.setId(videoId);
        video.setUserId(user.getId());
        video.setTitle(videoMetadata.getTitle());
        video.setDescription(videoMetadata.getDescription());
        video.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        video.setThumbnail(videoMetadata.getThumbnail());
        video.setTags(videoMetadata.getTags());
        video.setChannelName(videoMetadata.getChannelName());
        video.setUsername(videoMetadata.getUsername());
        video.setVideoStatus(videoMetadata.getVideoStatus());
        Video savedVideo = videoRepository.save(video);
        log.debug("Saved video with videoId: {}", savedVideo.getId());
    }

    @Override
    @Transactional
    public void incrementViews(String videoId) {
        log.debug("Incrementing views...");
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        video.incrementViews();
        videoRepository.save(video);
    }

    @Override
    @Transactional
    public AddCommentResponseModel addCommentToVideo(String videoId, CommentRequestModel commentRequestModel) {
        log.debug("Adding comment to the video with ID: {}", videoId);
        Video video = getVideoByIdHelper(videoId);
        Comment comment = commentMapper.commentRequestModelToComment(commentRequestModel);
        comment.setId(UUID.randomUUID().toString());
        comment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        video.addComment(comment);
        Video savedVideo = videoRepository.save(video);
        log.debug("Added comment to the video with comment ID: {}", comment.getId());
        Comment savedComment = savedVideo.getComments().stream()
                .filter(c -> c.getId().equals(comment.getId()))
                .findFirst()
                .orElseThrow(() -> new MongoWriteException("Comment is not added"));
        return commentMapper.commentToAddCommentResponseModel(savedComment);
    }

    @Override
    @Transactional
    public void updateComment(String videoId, String commentId, UpdateCommentRequestModel updateComment) {
        log.debug("Updating comment with ID: {} with video ID: {}", commentId, videoId);
        Video video = getVideoByIdHelper(videoId);
        Comment comment = video.getComments().stream().filter(c -> c.getId().equals(commentId))
                .findFirst().orElseThrow();
        comment.setBody(updateComment.getBody());
        videoRepository.save(video);
        log.debug("Updated comment...");
    }

    @Override
    @Transactional
    public void deleteComment(String videoId, String commentId) {
        log.debug("Deleting comment with ID: {} from video with ID: {}", commentId, videoId);
        Video video = getVideoByIdHelper(videoId);
        boolean status = video.getComments().removeIf(c -> c.getId().equals(commentId));
        videoRepository.save(video);
        if (status)
            log.debug("Deleted comment with ID: {}", commentId);
        else
            log.debug("Comment with ID: {} not found", commentId);
    }

    private Pageable getAllVideosPageRequest(int page) {
        return PageRequest.of(page, PAGE_OFFSET);
    }

    private Pageable getAllTrendingVideosPageRequest(int page) {
        return PageRequest.of(page, PAGE_OFFSET, Sort.Direction.DESC, "views");
    }

    private Video getVideoByIdHelper(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }
}
