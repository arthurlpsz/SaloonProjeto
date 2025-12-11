# Saloon ✂️
Aplicativo para **agendamento de serviços em salões de beleza**.

O **SaloonProjeto** foi desenvolvido para simplificar a experiência de clientes e salões, permitindo que usuários encontrem serviços, escolham horários disponíveis e realizem agendamentos de forma prática e intuitiva.  
O projeto também inclui recursos de gestão para salões cadastrados.

---

## 🎯 Funcionalidades do app:

### 👤 **Clientes:**
- **Criar uma conta** e acessar o app com segurança.
- **Buscar salões cadastrados** e visualizar informações como:
  - Nome do salão  
  - Serviços oferecidos  
  - Preços  
  - Disponibilidade  
- **Selecionar um serviço** (ex.: corte, barba, escova, etc.).
- **Escolher o horário disponível** conforme a agenda do salão.
- **Realizar agendamentos** rapidamente.
- **Visualizar seus agendamentos futuros**.
- **Cancelar agendamentos**, caso necessário.
- Acompanhar o **status do agendamento**.

---

### 💈 **Salões:**
- Criar ou acessar sua conta de salão.
- **Cadastrar e gerenciar serviços**:
  - Nome do serviço  
  - Preço  
  - Descrição  
- **Gerenciar sua agenda**, liberando horários disponíveis.
- **Visualizar todos os agendamentos recebidos**.
- Controlar cancelamentos e reorganizar horários.
- Ter uma visão clara do **dia de trabalho**, com clientes e horários organizados.

---

## 🧱 Principais características técnicas

- Desenvolvido em **Kotlin**.
- Integração com **Firebase Firestore**, usando:
  - Coleções como *Usuarios*, *Salao*, *Servicos*, *Agendamentos*.
  - Estrutura pensada para segurança e escalabilidade.
- Arquitetura organizada em *Models*, *Fragments* e regras de Firestore.
- Uso de **RecyclerViews**, **ViewBinding**, **Fragments** e boas práticas de navegação.

---

## 🚀 Como rodar o projeto

```bash
git clone https://github.com/arthurlpsz/SaloonProjeto.git
cd SaloonProjeto
./gradlew build
