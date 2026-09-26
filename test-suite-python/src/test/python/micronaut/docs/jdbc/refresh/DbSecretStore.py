from jakarta.inject import Singleton


@Singleton
class DbSecretStore:
    """Stands in for an external secret service (Vault, a cloud secret manager, ...) in the documentation example."""

    def __init__(self):
        self._password: str | None = None

    def current_password(self) -> str | None:
        return self._password

    def rotate(self, new_password: str) -> None:
        self._password = new_password
