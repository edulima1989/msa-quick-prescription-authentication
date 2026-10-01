INSERT INTO users (user_name, user_mail, user_password, user_role)
VALUES
    ('Administrador', 'admin@quickprescription.com', '$2b$12$.v1/uhDzz6881u4KzbmRl.Nfey4xIK3LzCCurP4SjAIiWIOFu3J7i', 'ADMIN'),
    ('Usuario Final', 'usuario.final@quickprescription.com', '$2b$12$ZlblcCareCRoW0PqzvBDQ.C2t22AzmSmxYm0Ps48I76LZgv8AQL3m', 'USUARIO_FINAL'),
    ('Usuario Final 2', 'usuario.final2@quickprescription.com', '$2b$12$ZlblcCareCRoW0PqzvBDQ.C2t22AzmSmxYm0Ps48I76LZgv8AQL3m', 'USUARIO_FINAL'),
    ('Usuario Final 3', 'usuario.final3@quickprescription.com', '$2b$12$ZlblcCareCRoW0PqzvBDQ.C2t22AzmSmxYm0Ps48I76LZgv8AQL3m', 'USUARIO_FINAL')
ON CONFLICT (lower(user_mail)) DO NOTHING;
