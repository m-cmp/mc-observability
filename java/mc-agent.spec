Name:           mc-agent
Version:        v0.2.0
Release:        1%{?dist}
Summary:    M-CMP Observability Agent RPM package

License:        Apache 2.0
URL:            https://github.com/m-cmp/mc-observability
Source0:        %{name}-%{version}.tar.gz


%description
M-CMP Observability Agent RPM
Built-in JDK 17, Telegraf 1.26.1

%prep
%setup -q


%post
test -d /etc/%{name} || {
    mkdir -p /etc/%{name}
}

if ! grep "^%{name}:" /etc/group &>/dev/null; then
    groupadd -r %{name}
fi

if ! id %{name} &>/dev/null; then
    useradd -r -M %{name} -s /bin/false -d /etc/%{name} -g %{name}
fi

LOG_DIR=/var/log/%{name}
mkdir -p $LOG_DIR
chown -R -L %{name}:%{name} $LOG_DIR
chmod 755 $LOG_DIR

chown -R -L %{name}:%{name} %{_bindir}/%{name}
chmod 755 -R %{_bindir}/%{name}

chown -R -L root:%{name} $RPM_BUILD_ROOT/usr/lib/%{name}

if [[ -d /run/systemd/system ]]; then
    systemctl enable %{name}
    systemctl daemon-reload
fi

test -e /etc/%{name}/uuid || {
    touch /etc/%{name}/uuid
    uuid=$(openssl rand -hex 16)
    echo ${uuid:0:8}-${uuid:8:4}-${uuid:12:4}-${uuid:16:4}-${uuid:20:12} > /etc/%{name}/uuid
    chown -R -L %{name}:%{name} /etc/%{name}
}

chown -R root:%{name} /usr/lib/%{name}
chmod 755 -R /usr/lib/%{name}

echo '%{name} package installed'
echo '-------------README-------------'
cat /usr/share/doc/%{name}/README
echo '--------------------------------'


%postun
if [[ -d /run/systemd/system ]]; then
    systemctl disable %{name}
    systemctl stop %{name}
fi

if grep "^%{name}:" /etc/group &>/dev/null; then
    groupdel -f %{name}
fi

if id %{name} &>/dev/null; then
    userdel -r %{name}
fi

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/%{_bindir}/%{name}
mkdir -p $RPM_BUILD_ROOT/usr/lib/systemd/system/
mkdir -p $RPM_BUILD_ROOT/usr/share/doc/%{name}
cp %{name} $RPM_BUILD_ROOT/%{_bindir}/%{name}
cp %{name}-collector $RPM_BUILD_ROOT/%{_bindir}/%{name}
cp %{name}.service $RPM_BUILD_ROOT/usr/lib/systemd/system/

LIB_DIR=$RPM_BUILD_ROOT/usr/lib/%{name}
mkdir -p $LIB_DIR/
tar xfpz jdk-17.tar.gz -C $LIB_DIR/ --strip-components=1
chmod -R 644 $LIB_DIR

cp setup $RPM_BUILD_ROOT/%{_bindir}/%{name}

cp README $RPM_BUILD_ROOT/usr/share/doc/%{name}/

if [[ -L /etc/init.d/%{name} ]]; then
    rm -f /etc/init.d/%{name}
fi

%files
%doc /usr/share/doc/%{name}/README
/usr/lib/%{name}/
%{_bindir}/%{name}/
/usr/lib/systemd/system/%{name}.service


%changelog
