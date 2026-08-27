package de.frauas.design.backend.desktop.repository;

import de.frauas.design.backend.desktop.model.Desktop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data repository for {@link Desktop} persistence. */
@Repository
public interface DesktopRepository extends JpaRepository<Desktop, Integer> {}
