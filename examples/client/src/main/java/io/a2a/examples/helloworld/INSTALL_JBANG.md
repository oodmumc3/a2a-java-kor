# JBang 설치

[JBang](https://www.jbang.dev/)은 자바 코드를 별도의 설치 없이 쉽게 실행할 수 있도록 돕는 도구입니다. 이 가이드는 다양한 플랫폼에서 JBang을 빠르게 설치하는 방법을 제공합니다.

## Linux 및 macOS

`curl` 또는 `wget`을 사용하여 JBang을 설치할 수 있습니다.

```bash
# curl 사용
curl -Ls https://sh.jbang.dev | bash -s - app setup

# 또는 wget 사용
wget -q https://sh.jbang.dev -O - | bash -s - app setup
```

설치 후에는 터미널을 다시 시작하거나 셸 설정 파일을 소스해야 할 수 있습니다.

```bash
source ~/.bashrc   # Bash용
source ~/.zshrc    # Zsh용
```

## Windows

### PowerShell 사용

```powershell
iex "& { $(iwr https://ps.jbang.dev) } app setup"
```

### Chocolatey 사용

```powershell
choco install jbang
```

### Scoop 사용

```powershell
scoop install jbang
```

## 설치 확인

JBang이 올바르게 설치되었는지 확인하려면 다음을 실행합니다.

```bash
jbang --version
```

JBang 버전 번호가 표시되어야 합니다.

## 추가 정보

더 자세한 설치 지침 및 옵션은 [JBang 설치 문서](https://www.jbang.dev/documentation/guide/latest/installation.html)를 참조하세요.
