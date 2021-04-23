Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "16384"
    vb.cpus = 6
  end
  config.vm.provision "shell", inline: <<-SHELL
    /vagrant/install-gms-prereqs vagrant
  SHELL
end
