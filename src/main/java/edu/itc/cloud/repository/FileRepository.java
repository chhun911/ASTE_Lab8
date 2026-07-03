package edu.itc.cloud.repository;

import edu.itc.cloud.model.FileEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByOwnerIdOrderByNameAsc(Long ownerId);

    List<FileEntity> findByOwnerIdAndFolderIdOrderByNameAsc(Long ownerId, Long folderId);

    void deleteByOwnerId(Long ownerId);
}
