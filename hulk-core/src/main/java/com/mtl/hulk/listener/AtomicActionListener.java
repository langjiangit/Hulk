package com.mtl.hulk.listener;

import com.mtl.hulk.HulkListener;
import com.mtl.hulk.HulkResourceManager;
import com.mtl.hulk.context.*;
import com.mtl.hulk.exception.HulkException;
import com.mtl.hulk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AtomicActionListener extends HulkListener {

    private volatile AtomicAction tryAction;

    private final Logger logger = LoggerFactory.getLogger(AtomicActionListener.class);

    public AtomicActionListener(AtomicAction action, ApplicationContext applicationContext, AtomicAction tryAction) {
        super(action, applicationContext);
        this.tryAction = tryAction;
    }

    @Override
    public boolean process() throws Exception {
        if (action.getServiceOperation().getType() == ServiceOperationType.TCC) {
            BusinessActivityContext bac = BusinessActivityContextHolder.getContext();
                Object object = null;
                try {
                    if (applicationContext.get().getId().split(":")[0].equals(action.getServiceOperation().getService())) {
                        object = applicationContext.get().getBean(tryAction.getServiceOperation().getBeanClass());
                    } else {
                        object = HulkResourceManager.getClients().get(action.getServiceOperation().getService());
                    }
                    logger.info("Transaction Executor running: {}", action.getServiceOperation().getName());
                    Method method = object.getClass().getMethod(action.getServiceOperation().getName(), BusinessActivityContext.class);
                    Object ret = method.invoke(object, bac);
                    if (((boolean) ret) == false) {
                        return false;
                    }
                } catch (InvocationTargetException ex) {
                    throw new HulkException(action.getServiceOperation().getName(), ex);
                } catch (Exception ex) {
                    throw new HulkException(action.getServiceOperation().getName(), ex);
                }
        }
        return true;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void destroyNow() {
    }

}
