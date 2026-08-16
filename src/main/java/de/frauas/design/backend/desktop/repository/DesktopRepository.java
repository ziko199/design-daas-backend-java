package de.frauas.design.backend.desktop.repository;

import de.frauas.design.backend.desktop.model.Desktop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesktopRepository extends JpaRepository<Desktop, Integer> {
}
