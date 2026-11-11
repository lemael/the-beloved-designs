package com.beloveddesigns.favorite.repository;

import com.beloveddesigns.favorite.entity.Favorite;
import com.beloveddesigns.favorite.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    List<Favorite> findByUserId(Long userId);
}
