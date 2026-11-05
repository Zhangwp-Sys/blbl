package com.yilena.service.dao;

import com.yilena.service.entity.dto.FavoritesFolderDTO;
import com.yilena.service.entity.po.FavoritesFolder;
import com.yilena.service.entity.vo.FavoritesFolderVO;
import com.yilena.service.entity.vo.VideoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 收藏夹文件夹数据访问接口
 * 
 * 功能说明：
 * 1. 提供收藏夹的增删改查
 * 2. 支持查询用户自己的收藏夹
 * 3. 支持查询他人的收藏夹
 * 4. 支持获取已订阅的收藏夹列表
 * 
 */
@Mapper
public interface FavoritesFolderMapper {
    void addFavoritesFolder(FavoritesFolder favoritesFolder);

    List<FavoritesFolderVO> getFavoritesFolderByList(FavoritesFolder favoritesFolder);

    void updateFavoritesFolder(FavoritesFolder favoritesFolder);

    FavoritesFolder getFavoritesFolderById(Long favoritesId);

    List<FavoritesFolderVO> getOthersFavoritesFolderByList(FavoritesFolder favoritesFolder);

    void deleteFavoritesFolder(Long id);

    List<FavoritesFolderVO> getSubscribeFavoritesFolder(Long userId);
}
