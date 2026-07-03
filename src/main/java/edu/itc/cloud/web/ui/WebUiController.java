package edu.itc.cloud.web.ui;

import edu.itc.cloud.model.User;
import edu.itc.cloud.service.QuotaExceededException;
import edu.itc.cloud.service.StorageService;
import edu.itc.cloud.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thin server-rendered UI (Thymeleaf) over the same services as the REST API.
 * The signed-in user is remembered in the HTTP session so a browser — and a
 * browser-driven Playwright test — can exercise the app end to end.
 */
@Controller
public class WebUiController {

    private static final String SESSION_UID = "uid";

    private final UserService users;
    private final StorageService storage;

    public WebUiController(UserService users, StorageService storage) {
        this.users = users;
        this.storage = storage;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return session.getAttribute(SESSION_UID) == null ? "redirect:/login" : "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session,
                           RedirectAttributes flash) {
        try {
            User user = users.register(email, password);
            session.setAttribute(SESSION_UID, user.getId());
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes flash) {
        try {
            User user = users.login(email, password);
            session.setAttribute(SESSION_UID, user.getId());
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "folder", required = false) Long folderId,
                            HttpSession session,
                            Model model) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        long used = storage.usedBytes(user);
        model.addAttribute("user", user);
        model.addAttribute("usedMb", String.format("%.1f", used / 1_048_576.0));
        model.addAttribute("quotaMb", Math.round(user.getQuotaBytes() / 1_048_576.0));
        model.addAttribute("usedPercent", Math.min(100, Math.round(used * 100.0 / user.getQuotaBytes())));
        model.addAttribute("currentFolder", folderId);
        model.addAttribute("folders", storage.listFolders(user, folderId));
        model.addAttribute("files", storage.listFiles(user, folderId));
        return "dashboard";
    }

    @PostMapping("/folders")
    public String createFolder(@RequestParam String name,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               HttpSession session,
                               RedirectAttributes flash) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            storage.createFolder(user, name, parentId);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToFolder(parentId);
    }

    @PostMapping("/files")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "folderId", required = false) Long folderId,
                         HttpSession session,
                         RedirectAttributes flash) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            storage.upload(user, folderId, file.getOriginalFilename(), file.getBytes());
        } catch (QuotaExceededException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            flash.addFlashAttribute("error", "Could not read the uploaded file");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToFolder(folderId);
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return ResponseEntity.status(302).header("Location", "/login").build();
        }
        byte[] bytes = storage.download(user, id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/files/{id}/delete")
    public String deleteFile(@PathVariable Long id,
                             @RequestParam(value = "folderId", required = false) Long folderId,
                             HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        storage.deleteFile(user, id);
        return redirectToFolder(folderId);
    }

    @PostMapping("/folders/{id}/delete")
    public String deleteFolder(@PathVariable Long id,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        storage.deleteFolder(user, id);
        return redirectToFolder(parentId);
    }

    @PostMapping("/account/delete")
    public String deleteAccount(HttpSession session) {
        User user = currentUser(session);
        if (user != null) {
            users.deleteAccount(user.getId());
        }
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private User currentUser(HttpSession session) {
        Object uid = session.getAttribute(SESSION_UID);
        if (uid == null) {
            return null;
        }
        try {
            return users.require((Long) uid);
        } catch (RuntimeException ex) {
            session.invalidate();
            return null;
        }
    }

    private String redirectToFolder(Long folderId) {
        return folderId == null ? "redirect:/dashboard" : "redirect:/dashboard?folder=" + folderId;
    }
}
