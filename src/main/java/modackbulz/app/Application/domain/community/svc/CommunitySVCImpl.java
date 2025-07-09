package modackbulz.app.Application.domain.community.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.community.dao.CommunityDAO;
import modackbulz.app.Application.entity.Community;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunitySVCImpl implements CommunitySVC {

  private final CommunityDAO communityDao;

  @Override
  public List<Community> getAllPosts() {
    return communityDao.findAll();
  }

  @Override
  public Optional<Community> getPostById(Long id) {
    return communityDao.findById(id);
  }

  @Override
  public Community createPost(Community community) {
    return communityDao.save(community);
  }

  @Override
  public Community updatePost(Community community) {
    return communityDao.update(community);
  }

  @Override
  public void deletePost(Long id) {
    communityDao.delete(id);
  }

  @Override
  public List<Community> getPostsByMemberId(Long memberId) {
    return communityDao.findByMemberId(memberId);
  }

  @Override
  public void increaseViewCount(Long id) {
    communityDao.increaseViewCount(id);
  }
}
