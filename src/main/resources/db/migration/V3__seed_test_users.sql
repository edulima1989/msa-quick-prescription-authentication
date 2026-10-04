-- Migración intencionalmente vacía (no-op).
--
-- Los usuarios de prueba para la integración front-back se crean vía API con
-- POST /api/v1/auth/register, de modo que la contraseña se cifre con el mismo
-- PasswordEncoder del backend y el login quede garantizado.
--
-- No se insertan hashes BCrypt escritos a mano aquí para evitar logins que fallan
-- en silencio por un hash mal construido.

SELECT 1;
