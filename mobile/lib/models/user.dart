class UserProfile {
  const UserProfile({
    required this.id,
    required this.username,
    required this.email,
    required this.displayName,
    this.avatarUrl,
    required this.xp,
    required this.roles,
  });

  final int id;
  final String username;
  final String email;
  final String displayName;
  final String? avatarUrl;
  final int xp;
  final Set<String> roles;

  bool get isAdmin => roles.contains('ADMINISTRATOR');
  bool get isModerator => roles.contains('MODERATOR') || isAdmin;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] as int,
      username: json['username'] as String,
      email: json['email'] as String? ?? '',
      displayName: (json['displayName'] ?? json['username']) as String,
      avatarUrl: json['avatarUrl'] as String?,
      xp: (json['xp'] as num?)?.toInt() ?? 0,
      roles: ((json['roles'] as List?) ?? const [])
          .map((e) => e.toString())
          .toSet(),
    );
  }
}

class AuthTokens {
  const AuthTokens({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final UserProfile user;

  factory AuthTokens.fromJson(Map<String, dynamic> json) {
    return AuthTokens(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      user: UserProfile.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}
