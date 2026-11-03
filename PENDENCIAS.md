# ESC Contábil: pendências (para continuar em outra IA)

## Contexto para colar na IA
Projeto Java 17 / Spring Boot 3.4 (Maven), pacote `com.jsb.recebimentos`, para controle de recebíveis da
ESC MANUTENCAO E PRESTACAO DE SERVICOS LTDA. Importa XML de NF-e, controla parcelas/vencimentos/pagamentos,
exporta XLSX/CSV (Power BI). Segurança com login + MFA (TOTP). Banco: H2 em dev, MySQL local em produção
(Flyway em `src/main/resources/db/migration/mysql`; novas mudanças = novas migrações V3__, V4__...).
Instalador Windows via jpackage (`installer/`). Leia o README.md antes de alterar. Rode `mvn test` ao final.
Não coloque senhas no Git. Não mude o esquema sem migração Flyway.

## Tarefas, em ordem de prioridade
1. **Validar o instalador** na máquina da empresa (ver `installer/LEIA-ME-INSTALACAO.md`) e corrigir o que falhar.
2. **Backup automático do MySQL**: script `.bat` com `mysqldump` + cópia de `%USERPROFILE%\.esc-contabil`,
   agendado no Agendador de Tarefas do Windows. Pronto quando: roda sozinho 1x/dia e mantém os últimos 14.
3. **Histórico de recebimentos por parcela** (hoje guarda só o total acumulado e a última data).
   Nova entidade `Payment` + migração V3 + ajustar `InvoiceService`, `Installment` e a tela.
4. **Importar extrato bancário (OFX/CSV)** e sugerir conciliação por valor/data/descrição, com confirmação humana.
5. **Trilha de auditoria** (quem registrou/alterou o quê e quando) e **perfis de usuário** (admin / somente leitura).
6. **Revisar vencimento provisório** de 30 dias quando a NF não traz duplicatas (confirmar regra com a empresa).
7. Confirmar se as notas são NF-e (produto) ou NFS-e (serviço) e ajustar o importador se for NFS-e.
8. Rodar como serviço do Windows (sem janela preta), por exemplo com WinSW ou NSSM.

## Pronto quando
`mvn clean verify` passa, o instalador gera e o fluxo "importar XML → registrar pagamento → exportar CSV"
funciona na máquina da empresa.
