package com.lms.adminpages.posts.controller;

import com.lms.adminpages.posts.entity.Post;
import com.lms.adminpages.posts.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("/posts-manage")
    public String listPosts(
            @RequestParam(value="category", required=false) String category,
            @RequestParam(value="type", required=false) String type,
            @RequestParam(value="keyword", required=false) String keyword,
            Model model) {

        List<Post> posts = postService.searchPosts(category, null, type, keyword);

        model.addAttribute("posts", posts);
        model.addAttribute("category", category);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        return "adminpages/posts-manage/index";
    }


    @GetMapping("/posts-manage/{id}")
    public String viewPost(@PathVariable("id") Integer postId, Model model) {
        Post post = postService.getPostById(postId);
        if (post == null) {
            return "redirect:/admin/posts-manage";
        }
        model.addAttribute("post", post);
        return "adminpages/posts-manage/view";
    }

    // 선택 삭제 (POST 요청)
    @PostMapping("/posts-manage/delete")
    public String deletePosts(@RequestParam("postIds") List<Integer> postIds, RedirectAttributes ra) {
        postService.deletePosts(postIds);
        ra.addFlashAttribute("message", postIds.size() + "개의 게시글이 삭제되었습니다.");
        return "redirect:/admin/posts-manage";
    }


    // 게시글 작성 폼
    @GetMapping("/posts-manage/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "adminpages/posts-manage/new"; // Thymeleaf 폼 경로
    }

    // 게시글 작성 처리
    @PostMapping("/posts-manage/new")
    public String createPost(@ModelAttribute Post post, RedirectAttributes ra) {
        // 작성자 ID는 예시로 1로 넣음 (실제 로그인 사용자 ID로 바꿔야 함)
        post.setUserId(1);
        post.setViews(0);
        post.setLikes(0);
        post.setCommentsCount(0);
        post.setIsDeleted(false);
        post.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        post.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        postService.savePost(post);
        ra.addFlashAttribute("message", "새 글이 등록되었습니다.");
        return "redirect:/admin/posts-manage";
    }
}
