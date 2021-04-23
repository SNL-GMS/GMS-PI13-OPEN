import datetime

from sqlalchemy import Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base

from . import engine
from . import Session
    
# State Database Keys
STATE='state'
START_TIME='start time'

# Application States
STATE_STARTED   = 'started'   # started, but NO config has yet been loaded to the system
STATE_LOADING   = 'loading'   # config is in the process of being loaded from this service to the system
STATE_LOADED    = 'loaded'    # config has been loaded to the system (at least once)

# Application States Transitions
#
#    load requested:
#       STARTED  > LOADING  (queue load: load will be successful/unsucessful)
#       LOADING  > LOADING  (return success, load already in progress)
#       LOADED   > LOADED   (return FAILURE, initial load already done)
#
#    reload requested:
#       STARTED  > STARTED  (return FAILURE, initial load not yet done)
#       LOADING  > LOADING  (return FAILURE, initial load in progress)
#       LOADED   > LOADING  (queue reload: reload will be successful/unsuccessful)
#
#    load complete (note: state should stay LOADING once queued)
#       if required  services not yet available, requeue load in 5 seconds (state remains LOADING)
#       LOADING  > STARTED (if load fails)
#       LOADING  > LOADED  (if load succeeds)
#
#    reload complete (note: state should stay LOADING once queued)
#       if required  services not yet available, requeue load in 5 seconds (state remains LOADING)
#       LOADING  > LOADED (if reload fails)
#       LOADING  > LOADED (if reload succeeds)
#

Base = declarative_base()

class StateKeyValue(Base):
    __tablename__ = 'application_state'
    key = Column(String(256), unique=True, primary_key=True)
    value = Column(String(1024))

    def __repr__(self):
        return '"%s": "%s"' % (self.key, self.value)

    
class State():
    def __init__(self):
        Base.metadata.drop_all(bind=engine)
        Base.metadata.create_all(bind=engine)
        
        session = Session()
        # session.drop_all()
        # session.create_all()
        session.add(StateKeyValue(key=START_TIME, value=str(datetime.datetime.now())))
        session.add(StateKeyValue(key=STATE, value=STATE_STARTED))
        session.commit()
        session.close()

    def get_state(self):
        session = Session()
        s = session.query(StateKeyValue).filter_by(key=STATE).first()
        session.close()
        return s.value
    
    def started(self):
        return self.get_state() == STATE_STARTED

    def loading(self):
        return self.get_state() == STATE_LOADING
    
    def loaded(self):
        return self.get_state() == STATE_LOADED

    def set_started(self):
        session = Session()
        s = session.query(StateKeyValue).filter_by(key=STATE).first()
        s.value = STATE_STARTED
        session.commit()
        session.close()
    
    def set_loading(self):
        session = Session()
        s = session.query(StateKeyValue).filter_by(key=STATE).first()
        s.value = STATE_LOADING
        session.commit()
        session.close()

    def set_loaded(self, reload=False):
        session = Session()
        s = session.query(StateKeyValue).filter_by(key=STATE).first()
        s.value = STATE_LOADED
        session.commit()
        session.close()
        
    def as_dict(self):
        session = Session()
        d = {}
        for item in session.query(StateKeyValue).all():
            d[item.key] = item.value
        session.close()
        return d
    
    def __repr__(self):
        session = Session()
        s = ""
        for item in session.query(StateKeyValue).all():
            s += str(item) + '\n'
        session.close()
        return s
