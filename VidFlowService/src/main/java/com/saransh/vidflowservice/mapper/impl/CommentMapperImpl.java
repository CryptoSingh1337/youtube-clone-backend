package com.saransh.vidflowservice.mapper.impl;

import com.saransh.vidflowdata.entity.Comment;
import com.saransh.vidflownetwork.request.video.CommentRequestModel;
import com.saransh.vidflownetwork.response.video.AddCommentResponseModel;
import com.saransh.vidflowservice.mapper.CommentMapper;
import org.springframework.stereotype.Component;

/**
 * @author CryptoSingh1337
 */
@Component
public class CommentMapperImpl implements CommentMapper {

    @Override
    public Comment commentRequestModelToComment(CommentRequestModel commentRequestModel) {
        if (commentRequestModel == null) {
            return null;
        }

        Comment.CommentBuilder comment = Comment.builder();

        comment.username(commentRequestModel.getUsername());
        comment.channelName(commentRequestModel.getChannelName());
        comment.body(commentRequestModel.getBody());

        return comment.build();
    }

    @Override
    public AddCommentResponseModel commentToAddCommentResponseModel(Comment comment) {
        if (comment == null) {
            return null;
        }

        AddCommentResponseModel.AddCommentResponseModelBuilder addCommentResponseModel = AddCommentResponseModel.builder();

        addCommentResponseModel.id(comment.getId());
        addCommentResponseModel.username(comment.getUsername());
        addCommentResponseModel.channelName(comment.getChannelName());
        addCommentResponseModel.body(comment.getBody());
        addCommentResponseModel.createdAt(comment.getCreatedAt());

        return addCommentResponseModel.build();
    }
}
