# Instalação do ESC Contábil (Windows)

## 1. Gerar o instalador (no seu computador)
Opção A (sem instalar nada): GitHub > Actions > **Installer** > Run workflow > baixe o artefato `esc-contabil-setup`.
Opção B (local): com JDK 17+, Maven e WiX 3.x instalados, rode `installer\build-installer.bat`. O `.exe` sai em `installer\output`.

## 2. Na máquina da empresa (uma vez)
1. Instalar **MySQL Community Server** como serviço automático do Windows.
2. Executar `database/mysql/setup.sql` no MySQL Workbench, **trocando a senha de exemplo** por uma senha forte.
3. Rodar o `Setup .exe` (o Java já vai embutido; não precisa instalar Java).
4. Criar a pasta `%APPDATA%\ESCContabil` e copiar para ela `installer\esc.properties.example` com o nome `esc.properties`. Editar e colocar a senha do passo 2.
5. Abrir **ESC Contabil** pelo atalho. Uma janela preta (logs) abre e depois o navegador em http://localhost:8080. Na primeira vez, criar o usuário em `/setup` e configurar o Authenticator (MFA).

## Cuidados
- **Não feche a janela preta** enquanto estiver usando o sistema (fechar encerra o sistema). Minimize.
- Sempre abra com o **mesmo usuário do Windows**. A chave que protege o MFA fica em `C:\Users\<usuario>\.esc-contabil\totp.key`. Se ela se perder, os usuários perdem o MFA e precisam ser refeitos.
- **Backup:** exporte o banco (`mysqldump esc_contabil`) e copie também a pasta `.esc-contabil`, para outro disco.
- Atualizar o sistema: instale a nova versão por cima. Os dados ficam no MySQL e não são apagados.
