import binascii
import io
import os
import re

import requests
from Crypto.Cipher import AES
from Crypto.Hash import SHA1  # or another digest, depending on `md` in Java
from Crypto.Util.Padding import unpad

NOFIM_PHONE_PREFIX = "026580"
CONTACT_ROW_PATTERN_V3 = "^(.*?)\\s+\\[X]\\s+(.*?)\\s+\\[Y]\\s+(.*?)\\[Z]\\s+(.*?)$"

HTML_TEMPLATE_PATH = "./nlistr_template.html"
HTML_OUTPUT_PATH = "/var/www/nimrodrak.com/nofim/html/index.html"

REMOTE_ENCRYPTED_DATABASE_V3_SERVER_URL = (
    "https://raw.githack.com/NimrodRak/nlist-updates/main/EncryptedDB3"
)

HTML_ROW_FORMAT = """
<a class="contact-row" href="tel:{phone_number_raw}">
  <span class="contact-phone">{phone_number_pretty}</span>
  <span class="contact-name">{name}</span>
  <span class="contact-room">{room_number}</span>
</a>
"""


def decrypt_stream(ciphertext, k1, digest_cls=SHA1, empty_line=""):
    # AES/ECB/PKCS5Padding in Java == AES-ECB with PKCS7 padding in PyCryptodome
    cipher = AES.new(k1, AES.MODE_ECB)

    # Decrypt and unpad
    plaintext_bytes = unpad(cipher.decrypt(ciphertext), AES.block_size)

    # Feed plaintext through a digest, like Java DigestInputStream
    digest = digest_cls.new()
    digest.update(plaintext_bytes)
    digest_value = digest.digest()  # equivalent of md.digest() after the read loop

    # Decode bytes as Windows-1255 (cp1255 in Python)
    text = plaintext_bytes.decode("cp1255")  # WINDOWS_1255_CHARSET

    # Now iterate line by line, stopping at EMPTY_LINE, just like the Java loop
    reader = io.StringIO(text)
    for line in reader:
        line = line.rstrip("\r\n")
        if line == empty_line:
            break
        # process `line` here
        yield line

    return digest_value  # return the computed message digest


def generate_contact_row(line):
    m = re.match(CONTACT_ROW_PATTERN_V3, line)
    return HTML_ROW_FORMAT.format(
        phone_number_raw=NOFIM_PHONE_PREFIX + m.group(3),
        room_number=m.group(1),
        name=m.group(2).replace("·", ""),
        phone_number_pretty=m.group(3)
    )


def main():
    aes_key = binascii.unhexlify(os.getenv("KEY"))

    remote_stream = requests.get(REMOTE_ENCRYPTED_DATABASE_V3_SERVER_URL).content

    decrypted_lines = decrypt_stream(remote_stream, aes_key)

    html_contact_rows = "\n".join(map(generate_contact_row, decrypted_lines))

    with open(HTML_TEMPLATE_PATH) as f:
        html_template = f.read()

    with open(HTML_OUTPUT_PATH, "w") as f:
        f.write(html_template.replace("MAGIC_PLACEHOLDER", html_contact_rows))


if __name__ == "__main__":
    main()
