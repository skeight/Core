package com.iConomy.system;

import com.iConomy.iConomy;
import com.iConomy.util.Messaging;
import com.iConomy.util.Template;

import org.bukkit.entity.Player;

public class Account {
    String name;

    public Account(String name) {
        this.name = name;
    }

    public Account(String name, Boolean create) {
        this.name = name;
    }

    public void showHoldings(boolean console) {
        if(console)
            return;

        Player player = iConomy.Server.getPlayer(name);
        if(iConomy.Server.getPlayer(name) == null)
            return;

        String tag = iConomy.Template.color(Template.Node.TAG_MONEY);

        Template template = iConomy.Template;
        template.set(Template.Node.PERSONAL_BALANCE);
        template.add("balance", getHoldings().getBalance());

        Messaging.send(player, tag + template.parse());
    }

    public Holdings getHoldings() {
        return new Holdings(this.name);
    }

    public boolean remove() {
        return Queried.removeAccount(this.name);
    }

    @Override
    public String toString() {
        String tag = iConomy.Template.raw(Template.Node.TAG_MONEY);

        Template template = iConomy.Template;
        template.set(Template.Node.PLAYER_BALANCE);
        template.add("name", name);
        template.add("balance", getHoldings().getBalance());

        return tag + template.parseRaw();
    }
}
