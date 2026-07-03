package edu.itc.cloud.repository;

import edu.itc.cloud.model.Folder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByOwnerIdOrderByNameAsc(Long ownerId);

    List<Folder> findByOwnerIdAndParentIdOrderByNameAsc(Long ownerId, Long parentId);

    void deleteByOwnerId(Long ownerId);
}
