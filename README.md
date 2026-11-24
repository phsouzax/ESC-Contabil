# ESC Contábil

Aplicação Java/Spring Boot para importar XML de NF-e, controlar vencimentos e pagamentos, exportar Excel/CSV e apoiar a rotina de recebíveis da ESC.

## Pré-requisitos

- JDK 17 ou superior (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Navegador moderno

## Desenvolvimento no computador do projeto

Por padrão, o perfil `dev` usa H2 em arquivo para facilitar o desenvolvimento no IntelliJ. No terminal, dentro da pasta do projeto:

```powershell
mvn spring-boot:run
```

Abra <http://localhost:8080>. O arquivo H2 fica em `data/recebimentos.mv.db` relativo à pasta de trabalho da aplicação; o perfil `dev` mantém o console em <http://localhost:8080/h2-console>. Esse perfil serve para desenvolvimento e não é usado na instalação local de produção.

Para executar a suíte automatizada de testes:

```powershell
mvn test
```

O workflow `.github/workflows/ci.yml` roda `clean verify` em Java 17 nos pushes e pull requests e guarda o JAR resultante como artefato temporário.

## Banco local de produção (MySQL Community)

A instalação final usa MySQL Community no próprio computador da empresa, sem mensalidade de hospedagem. O Windows inicia o MySQL como serviço; a aplicação conecta apenas a `127.0.0.1`, e o banco não precisa ser exposto à rede ou à internet.

1. Instale MySQL Community Server para Windows e configure-o como serviço automático.
2. Abra o MySQL Workbench ou o cliente MySQL como administrador e execute `database/mysql/setup.sql` **depois de substituir o texto de senha de exemplo por uma senha forte e exclusiva**. O script cria o banco `esc_contabil` e um usuário limitado a esse banco.
3. Configure `ESC_DB_PASSWORD` no Windows com a mesma senha criada no passo anterior. Não coloque a senha no Git nem em `application.properties`.
4. Inicie a aplicação com o perfil `mysql-local`, por exemplo: `java -jar controle-recebimentos.jar --spring.profiles.active=mysql-local`.

Para executar esse perfil pelo IntelliJ, use `SPRING_PROFILES_ACTIVE=mysql-local` na configuração de execução e mantenha `ESC_DB_PASSWORD` configurada no ambiente do Windows. A configuração de execução já fixa a pasta de trabalho na raiz do projeto para o perfil `dev`, mantendo estável o caminho relativo do H2 durante o desenvolvimento.

No primeiro início, Flyway aplica `src/main/resources/db/migration/mysql` e cria as tabelas. O Hibernate valida o esquema, sem alterá-lo automaticamente. As versões seguintes devem adicionar novas migrações `V2__...sql`, `V3__...sql` etc. O arquivo H2 de desenvolvimento continua separado e não é apagado nem copiado para o MySQL por esta configuração.

O serviço MySQL pode iniciar com o Windows, mas o computador precisa estar ligado para que o sistema seja usado. Faça backup do banco MySQL para outro disco ou local seguro; o arquivo do banco não deve ser salvo dentro da pasta do instalador.

## Usar

1. No Emissor Sebrae, exporte os XMLs em **Emissor Fiscal → Consulta de Documentos**. Prefira XML a PDF.
2. Na página, selecione um ou vários XMLs e clique **Importar XML**.
3. Confira cliente, número, emissão e parcelas extraídos. O app ignora NF repetida pelo número + documento do cliente.
4. Para registrar um recebimento, informe valor e data na linha da parcela e clique **Registrar**. Valor menor representa pagamento parcial; a parcela fica aberta e aceita outro recebimento depois.
5. Exporte um `.xlsx` formatado ou um `.csv` tabular para análise no Power BI.

## Campos e regras atuais

- NF-e: número, mês de referência inicial (mês de emissão), cliente/documento, modelo, data de emissão e duplicatas `cobr/dup` (`nDup`, `dVenc`, `vDup`).
- Sem duplicatas: cria uma parcela com o total da NF e vencimento provisório em 30 dias. **Esse vencimento deve ser revisado**: prazo real/parcelamento não pode ser inferido do XML fiscal quando não consta nele.
- Situação: `PAGO` quando o valor recebido cobre a parcela; `VENCIDO` quando não pago e vencimento passou; senão `A VENCER`.
- O CSV tem uma linha por parcela e inclui mês recebido para análise no Power BI.
- O XML original fica guardado junto à nota no banco para rastreabilidade.

## Integração com Power BI

Baixe o CSV e no Power BI Desktop use **Obter dados → Texto/CSV**, selecione UTF-8 e delimitador ponto e vírgula. A instalação local não publica o banco na internet; para atualização no Power BI Service será necessária uma estratégia de acesso separada.

## Estrutura

```text
src/main/java/com/jsb/recebimentos/
  RecebimentosApplication.java           inicialização Spring
  controller/WebController.java          rotas da interface
  entity/Invoice.java                    nota fiscal
  entity/Installment.java                parcela e situação calculada
  repository/InvoiceRepository.java      persistência
  service/InvoiceService.java            leitura segura do XML, importação e CSV
src/main/resources/
  application.properties        configurações compartilhadas
  application-dev.properties   H2 local para desenvolvimento
  application-mysql-local.properties  conexão MySQL local de produção
  db/migration/mysql/          migrações versionadas do banco de produção
  templates/index.html          interface web
```

## Limites desta versão

- Importa XML de NF-e; ainda não acessa automaticamente a conta Sebrae/SEFAZ, não lê PDF e não importa extrato bancário.
- A conciliação bancária ainda exige importador OFX/CSV. Pagamentos parciais são somados na parcela, mas esta versão guarda apenas o total acumulado e a data mais recente, sem histórico individual por recebimento.
- O perfil `dev` e o console H2 são somente para desenvolvimento; não os utilize na instalação final.
- Mês de referência começa igual ao mês de emissão e pode ser tratado/ajustado depois conforme a regra do negócio.

## Próximos passos para produção

1. Confirmar formato real das notas (NF-e de produto ou NFS-e de serviço), XML de exemplo sem dados sensíveis e regras de parcelas.
2. Implementar importação de extrato OFX/CSV e conciliação sugerida por valor/data/descrição com confirmação humana.
3. Adicionar autenticação, perfis, trilha de auditoria e rotina automatizada de backup.
4. Criar e validar o instalador Windows, preservando a pasta de dados MySQL nas atualizações.
