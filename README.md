# 🕹️ VERTICAOS DROID — Frontend Arcade Vertical para Android

**VERTICAOS DROID** é um launcher/frontend desenvolvido por **Caos Gamer** focado em fliperamas verticais (modo retrato — `720x1080` / `1080x1920`) rodando em **Android TV Box** e gabinetes arcade comerciais ou residenciais.

Projetado para operar **100% sem mouse ou teclado**, oferecendo uma interface robusta, rápida e à prova de falhas para uso contínuo em fliperamas, bartops e eventos.

---

## ✨ Principais Recursos

* **Orientação Vertical Nativa:** Interface otimizada para telas em pé (Portrait), ideal para jogos *endless runner*, clássicos verticais e adaptações arcade.
* **Previews em Vídeo Local:** Reprodução automática de vídeos de demonstração dos jogos armazenados localmente, sem depender de internet.
* **Lista de Jogos Customizável (`JOGOS.TXT`):** Adicione, remova ou reordene títulos facilmente editando um arquivo de texto simples, apontando diretamente para o *package name* de cada APK instalado.
* **Sistema de Fichas & Temporizador Configurável:**
  * Suporte a modo **Freeplay** (ideal para eventos, buffets e uso doméstico).
  * Modo **Comercial (Ficheiro / Moedeiro)** com pulso de crédito e contagem regressiva de tempo configurável na tela.
* **Navegação 100% Arcade (Zero Delay / Teclado):**
  * Controle total pelos direcionais e botões do comando arcade.
  * **Tecla/Botão 3 (Sair/Voltar):** Encerra o jogo em execução em segundo plano e retorna instantaneamente ao menu principal do frontend sem travamentos.

---

## 🛠️ Requisitos e Hardware Recomendado

Para evitar falhas de renderização de texturas (como tela rosa em jogos feitos na engine Unity), recomenda-se o uso de TV Boxes com chipsets testados e homologados:

* **Chipsets Recomendados:** Amlogic **S905L**, **S905W** (ex: TX9 P281) ou placas homologadas **Proeletronic (PROSB-3000)**.
* **Evitar:** Chipsets RK3539 ou variantes genéricas incompatíveis com shaders OpenGL ES do Unity.
* **Controles:** Placas encoder USB padrão **Zero Delay**, Raspberry Pi Pico (HID/Teclado) ou adaptadores JAMMA-USB.

---

## 📂 Estrutura de Configuração (`JOGOS.TXT`)

O sistema lê a lista de jogos e mídias a partir da pasta configurada no armazenamento do Android:

* **`JOGOS.TXT`**: Arquivo de mapeamento contendo o nome de exibição e o pacote Android (`com.exemplo.jogo`) de cada título.
* **`videos/`**: Pasta com os arquivos `.mp4` correspondentes a cada jogo da lista para exibição do preview.

---

## 👨‍💻 Autor & Créditos

Desenvolvido e mantido por **Caos Gamer**  
*Sistemas VERTICAOS & MULTICAOS — Soluções para Fliperamas e Multijogos*
<img width="1080" height="1920" alt="image" src="https://github.com/user-attachments/assets/731979ba-5dfd-4d2a-ad96-f107152cf75d" />

