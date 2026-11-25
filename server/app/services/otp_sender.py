import logging
import re
from abc import ABC, abstractmethod

logger = logging.getLogger("wegrow.otp")

PHONE_RE = re.compile(r"^\+?[0-9]{9,15}$")
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def normalize_identifier(raw: str) -> tuple[str, str]:
    """Normalize phone or email identifier and return (normalized_value, channel)."""
    cleaned = raw.strip()
    if "@" in cleaned:
        lowered = cleaned.lower()
        if not EMAIL_RE.match(lowered):
            raise ValueError("Invalid email address")
        return lowered, "email"

    digits = re.sub(r"[\s\-\(\)]", "", cleaned)
    if digits.startswith("998") and not digits.startswith("+"):
        digits = "+" + digits
    elif not digits.startswith("+") and len(digits) == 9:
        digits = "+998" + digits
    elif not digits.startswith("+"):
        digits = "+" + digits

    if not PHONE_RE.match(digits):
        raise ValueError("Invalid phone number (expected +998XXXXXXXXX or international format)")
    return digits, "phone"


class OtpSender(ABC):
    @abstractmethod
    def send(self, identifier: str, channel: str, code: str) -> None:
        raise NotImplementedError


class DevOtpSender(OtpSender):
    def send(self, identifier: str, channel: str, code: str) -> None:
        logger.info("[OTP:%s] Sent verification code %s to %s", channel, code, identifier)


def get_otp_sender() -> OtpSender:
    # Future production hook: switch to EskizSmsSender / SmtpEmailSender via settings
    return DevOtpSender()
