// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PatientenErfServerServiceLocator.java

package ch.ct.patientenerfassung.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.util.HashSet;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Service;
import org.apache.axis.client.Stub;

// Referenced classes of package ch.ct.patientenerfassung.client:
//            PatientenErfassungSoapBindingStub, PatientenErfServerService, PatientenErfServer

public class PatientenErfServerServiceLocator extends Service
    implements PatientenErfServerService
{

    public PatientenErfServerServiceLocator()
    {
        PatientenErfassungWSDDServiceName = "PatientenErfassung";
        ports = null;
    }

    public String getPatientenErfassungAddress()
    {
        return "https://estudio.clustertec.ch/axis/services/PatientenErfassung";
    }

    public String getPatientenErfassungWSDDServiceName()
    {
        return PatientenErfassungWSDDServiceName;
    }

    public void setPatientenErfassungWSDDServiceName(String name)
    {
        PatientenErfassungWSDDServiceName = name;
    }

    public PatientenErfServer getPatientenErfassung()
        throws ServiceException
    {
        URL endpoint;
        try
        {
            endpoint = new URL("https://estudio.clustertec.ch/axis/services/PatientenErfassung");
        }
        catch(MalformedURLException e)
        {
            PatientenErfServer patientenerfserver = null;
            return patientenerfserver;
        }
        return getPatientenErfassung(endpoint);
    }

    public PatientenErfServer getPatientenErfassung(URL portAddress)
        throws ServiceException
    {
        PatientenErfServer patientenerfserver;
        try
        {
            PatientenErfassungSoapBindingStub _stub = new PatientenErfassungSoapBindingStub(portAddress, this);
            _stub.setPortName(getPatientenErfassungWSDDServiceName());
            PatientenErfassungSoapBindingStub patientenerfassungsoapbindingstub = _stub;
            return patientenerfassungsoapbindingstub;
        }
        catch(AxisFault e)
        {
            patientenerfserver = null;
        }
        return patientenerfserver;
    }

    public Remote getPort(Class serviceEndpointInterface)
        throws ServiceException
    {
        try
        {
            if((ch.ct.patientenerfassung.client.PatientenErfServer.class).isAssignableFrom(serviceEndpointInterface))
            {
                PatientenErfassungSoapBindingStub _stub = new PatientenErfassungSoapBindingStub(new URL("https://estudio.clustertec.ch/axis/services/PatientenErfassung"), this);
                _stub.setPortName(getPatientenErfassungWSDDServiceName());
                PatientenErfassungSoapBindingStub patientenerfassungsoapbindingstub = _stub;
                return patientenerfassungsoapbindingstub;
            }
        }
        catch(Throwable t)
        {
            throw new ServiceException(t);
        }
        throw new ServiceException("There is no stub implementation for the interface:  ".concat(String.valueOf(String.valueOf(serviceEndpointInterface != null ? ((Object) (serviceEndpointInterface.getName())) : "null"))));
    }

    public Remote getPort(QName portName, Class serviceEndpointInterface)
        throws ServiceException
    {
        Remote _stub = getPort(serviceEndpointInterface);
        ((Stub)_stub).setPortName(portName);
        return _stub;
    }

    public QName getServiceName()
    {
        return new QName("https://estudio.clustertec.ch/axis/services/PatientenErfassung", "PatientenErfServerService");
    }

    public Iterator getPorts()
    {
        if(ports == null)
        {
            ports = new HashSet();
            ports.add(new QName("PatientenErfassung"));
        }
        return ports.iterator();
    }

    private final String PatientenErfassung_address = "https://estudio.clustertec.ch/axis/services/PatientenErfassung";
    private String PatientenErfassungWSDDServiceName;
    private HashSet ports;
}
