package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SecurityStateService securityStateService;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + phoneNumber));
        if (securityStateService.isTransitioning(user.getId())) {
            throw new LockedException("Account security information is being updated");
        }
        return new CustomUserDetails(user);
    }

    public CustomUserDetails getUserById(UUID id){
        User user =  userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return new CustomUserDetails(user);
    }
}
