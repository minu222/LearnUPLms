package com.lms.adminpages.posts.service;

import com.lms.adminpages.posts.dao.PostDAO;
import com.lms.adminpages.posts.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostDAO postDAO;

    public List<Post> searchPosts(String category, String state, String type, String keyword) {
        return postDAO.searchPosts(category, state, type, keyword);
    }

    public Post getPostById(Integer postId) {
        return postDAO.findById(postId);
    }

    public void deletePosts(List<Integer> postIds) {
        postDAO.markAsDeleted(postIds);
    }

    public void savePost(Post post) {
        postDAO.save(post);
    }
}