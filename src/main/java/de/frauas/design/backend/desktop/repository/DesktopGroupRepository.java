package de.frauas.design.backend.desktop.repository;

import de.frauas.design.backend.desktop.model.DesktopGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesktopGroupRepository extends JpaRepository<DesktopGroup, Integer> {}
