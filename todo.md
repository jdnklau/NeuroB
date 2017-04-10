# To Do List
- update java doc
- model handling
    - have list of preprocessing utilities that is managed by responsible class
- training
    - more generic handling of data preprocessing if possible
        - add more preprocessing tools
            - PCA: whitening, feature reduction
    - distinction between classification and regression
        - log confusion matrix
- training set generation
    - utility to truncate data sets with a highly uneven class distribution
    - shuffle data files
    - optimise
        - generation pipeline flushing samples directly to files instead of 
          holding everything in memory first
    - data augmentation for larger training sets
- training set analysis
    - t-SNE
    - PCA

# Future Work
- model check machines
    - generate 100 states per solver
        - give credit to fastest one
    - or: state space exploration over a fixed time
        - give credit to the one with the most states generated
